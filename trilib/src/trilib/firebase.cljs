;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilib.firebase
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.spec.alpha :as s]
   [lambdaisland.uri :as uri]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [vimsical.re-frame.cofx.inject :as inject]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
   [iron.utils :refer [ci-includes? validate]]
   [com.degel.re-frame-firebase :as firebase]
   [trilib.db :as db]
   [trilib.fsm :as fsm]))


(s/def ::uid string?)
(s/def ::provider-data any?)
(s/def ::display-name (s/nilable string?))
(s/def ::photo-url (s/nilable string?))
(s/def ::email string?)
(s/def ::user (s/nilable
               (s/keys :req-un [::uid ::provider-data ::display-name ::photo-url ::email])))

(s/def ::db-keys (s/keys :req [::user]))


;;; From https://console.firebase.google.com/u/0/project/trilystro/overview - "Add Firebase to your web app"
(defonce firebase-app-info
  {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
   :authDomain "trilystro.firebaseapp.com"
   :databaseURL "https://trilystro.firebaseio.com"
   :storageBucket "trilystro.appspot.com"})

;;; Magic Firebase object that the server translates into a timestamp
(defonce timestamp-marker
  (-> js/firebase .-database .-ServerValue .-TIMESTAMP))


(defn init []
  (re-frame/dispatch-sync [:trilib.fsm/goto :try-login])
  (firebase/init :firebase-app-info      firebase-app-info
                 :get-user-sub           [::user]
                 :set-user-event         [::set-user]
                 :default-error-handler  [:firebase-error]))

(defn private-fb-path
  ([path]
   (private-fb-path path (<sub [::uid])))
  ([path uid]
   (when uid
     (into [:private uid] path))))

(defn all-shared-fb-path
  [path]
  (into [:shared] path))


(defn my-shared-fb-path
  ([path]
   (my-shared-fb-path path (<sub [::uid])))
  ([path uid]
   (when uid
     (into [:shared (first path) uid] (rest path)))))

(defn public-fb-path [path]
  (if-let [uid (<sub [::uid])]
    (into [:public] path)))


(sub2 ::user [::user])
(sub2 ::uid  [::user :uid])

(re-frame/reg-sub
 ::user-name
 :<- [::user]
 (fn [user _]
   (or (:display-name user) (:email user))))



(re-frame/reg-event-fx
 ::poor-mans-set-user
 (fn [{db :db} [_ user]]
   (into {:db (assoc db ::user user)}
         (when user
           ;; [TODO][ch94] Should rename :user-details to :users-details in Firebase.
           ;;    Requires a migration hack.
           {:firebase/write {:path       (my-shared-fb-path [:user-details] (:uid user))
                             :value      (into {:timestamp timestamp-marker}
                                               (select-keys user [:display-name :email :photo-url]))
                             :on-success #(console :log "Logged in:" (:display-name user))
                             :on-failure #(console :error "Login failure: " %)}}))))

(re-frame/reg-event-fx
 ::set-user
 [db/check-spec-interceptor]
 (fn [{db :db} [_ user]]
   (into {:db (assoc db ::user user)
          :dispatch [:trilib.fsm/goto (if user :login-confirmed :logout)]}
         (when user
           ;; [TODO][ch94] Should rename :user-details to :users-details in Firebase.
           ;;    Requires a migration hack.
           {:firebase/write {:path       (my-shared-fb-path [:user-details] (:uid user))
                             :value      (into {:timestamp timestamp-marker}
                                               (select-keys user [:display-name :email :photo-url]))
                             :on-success #(console :log "Logged in:" (:display-name user))
                             :on-failure #(console :error "Login failure: " %)}}))))


(re-frame/reg-event-fx
 :sign-in
 [db/check-spec-interceptor]
 (fn [_ _] {:firebase/google-sign-in nil}))

(re-frame/reg-event-fx
 :sign-out
 [db/check-spec-interceptor]
 (fn [_ _] {:firebase/sign-out nil}))

(re-frame/reg-event-fx
 :firebase-error
 [db/check-spec-interceptor]
 (fn [_ [_ error]]
   (js/console.error (str "FIREBASE ERROR:\n" error))))


(re-frame/reg-event-fx
 :db-write-public
 [db/check-spec-interceptor]
 (fn [{db :db} [_ {:keys [path value on-success on-failure] :as args}]]
   (if-let [path (public-fb-path path)]
     {:firebase/write (assoc args :path path)}

     ;; [TODO] Need to use pending Iron generalization of :dispatch that takes a fn too.
     ((if on-failure (re-utils/event->fn on-failure) js/alert)
      (str "Can't write to Firebase, because not logged in:\n " path ": " value)))))

(defn logged-in? [db]
  (some? (get-in db [::user :uid])))

(defn fb-event [{:keys [db path value on-success on-failure access effect-type for-multi?] :as args}]
  (if (logged-in? db)
    (let [path ((case access
                  :public public-fb-path
                  :shared my-shared-fb-path
                  :private private-fb-path
                  identity) path)
          effect-args (assoc (select-keys args [:value :on-success :on-failure])
                             :path path)]
      (if for-multi?
        [effect-type effect-args]
        {effect-type effect-args}))

    ;; [TODO] Need to use pending Iron generalization of :dispatch that takes a fn too.
    ((if on-failure
       (re-utils/event->fn on-failure)
       js/alert)
     (str "Can't write to Firebase, because not logged in:\n " path ": " value))))


(re-frame/reg-sub
 ::user-settings
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path (private-fb-path [:user-settings])}]))
 (fn [settings [_ ks not-found]]
   (if ks
     (get-in settings ks not-found)
     settings)))


(re-frame/reg-event-fx
 ::commit-user-setting
 [db/check-spec-interceptor]
 (fn [{db :db} [_ setting value]]
   (fb-event
    {:for-multi? false
     :effect-type :firebase/write
     :db db
     :access :private
     :path [:user-settings setting]
     :value value})))


(re-frame/reg-sub
 ::users-details
 (fn [_ _]
   ;; [TODO][ch94] Rename :user-details to :users-details
   (re-frame/subscribe [:firebase/on-value {:path (all-shared-fb-path [:user-details])}]))
 (fn [details _]
   details))


(re-frame/reg-sub
 ::user-of-id
 (fn [_ _] (re-frame/subscribe [::users-details]))
 (fn [details [_ user-id]]
   (when user-id
     ((keyword user-id) details))))


(re-frame/reg-sub
 ::user-pretty-name
 (fn [[_ id]]
   (re-frame/subscribe [::user-of-id id]))
 (fn [user [_ _]]
   (or (:display-name user)
       (:email user))))


(defn lystro-commit-event [db
                           {:keys [firebase-id tags url text owner public? original-public?] :as lystro}
                           & {:keys [on-success]}]
  (let [exists? (some? firebase-id)
        changed-access? (and exists?
                             (some? original-public?)
                             (not (= original-public? public?)))]
    (fb-event {:db db
               :for-multi? false
               :effect-type (if exists? :firebase/write :firebase/push)
               :path (if exists? [:lystros firebase-id] [:lystros])
               :access (if public? :shared :private)
               :on-success #(do
                              (when changed-access?
                                ;; Private and public lystros are stored in different branches of the
                                ;; Firebase tree. So, if the access has changed, we need to explicitly
                                ;; delete the old one. To be safe, we only do so here, on success of
                                ;; writing the new one.
                                ;; [TODO][ch153] This whole thing should really be moved into a Firebase
                                ;; transaction. We are not protected against the broader set of problems
                                ;; that two clients might be modifying the same Lystro simultaneously
                                ;; so we need to check for any changes before writing. If done right,
                                ;; the access stuff should fall out nicely. See
                                ;; https://stackoverflow.com/questions/42183179/firebase-atmonic-add-and-delete-entry
                                ;; and
                                ;; https://firebase.google.com/docs/database/web/read-and-write#save_data_as_transactions
                                (>evt [::clear-lystro (assoc lystro :public? original-public?)]))
                              (when on-success
                                (on-success)))
               :value {:tags tags
                       :url (str url)
                       :text (str text)
                       :owner (str owner)
                       :public? public?
                       :timestamp timestamp-marker}})))

(re-frame/reg-event-fx
 ::commit-lystro
 [db/check-spec-interceptor]
 (fn [{db :db} [_
                {:keys [firebase-id tags url text owner public? original-public?] :as lystro}
                & {:keys [on-success]}]]
   (assoc (lystro-commit-event db lystro :on-success on-success)
          :dispatch [::commit-user-setting :default-public? public?])))


(re-frame/reg-event-fx
 ::clear-lystro
 [(re-frame/inject-cofx ::inject/sub [::uid])
  db/check-spec-interceptor]
 (fn [{db :db uid ::uid} [_ {:keys [firebase-id owner public?]} :as lystro]]
   (let [mine? (= owner uid)]
     (when mine?
       (fb-event {:for-multi? false
                  :effect-type :firebase/write
                  :db db
                  :access (if public? :shared :private)
                  :path [:lystros firebase-id]
                  :value nil})))))




(defn filter-some-tags
  "Filter function that selects lystros whose tags include at least
  one of the match-set"
  [match-set]
  {:pre [(validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [tags]}]
            (let [tags (set tags)
                  intersection (if (empty? match-set)
                                 tags
                                 (set/intersection tags match-set))]
              (or (empty? match-set)
                  (not (empty? intersection)))))))

(defn filter-all-tags
  "Filter function that selects lystros whose tags include all
  of the match-set"
  [match-set]
  {:pre [(validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [tags]}]
            (if (empty? match-set)
              true
              (= (set/intersection match-set (set tags))
                 match-set)))))

(defn filter-url-field
  "Filter function that selects lystros whose url field includes match-url."
  [match-url]
  {:pre [(validate string? match-url)]}
  (filter (fn [lystro]
            (let [lystro-url (or (:url lystro) "")]
              (if (empty? match-url)
                lystro-url
                (ci-includes? lystro-url match-url))))))

(defn filter-text-field
  "Filter function that selects lystros whose text field includes
  match-text or if the text is found in the tags or url, when them
  matching option has been passed in."
  [match-text tags-as-text? url-as-text?]
  {:pre [(validate string? match-text)]}
  (filter (fn [lystro]
            (if (empty? match-text)
              (:text lystro)
              (let [all-text
                    (str (when tags-as-text?
                           (->> (:tags lystro) (interpose " ") (apply str)))
                         " "
                         (when url-as-text?
                           (:url lystro))
                         " "
                         (:text lystro))]
                (ci-includes? all-text match-text))))))


(defn filter-lystros [lystros {:keys [tags-mode tags url text tags-as-text? url-as-text?]}]
  (transduce (comp (if (= tags-mode :all-of)
                     (filter-all-tags tags)
                     (filter-some-tags tags))
                   (filter-url-field (str/trim (or url "")))
                   (filter-text-field (str/trim (or text "")) tags-as-text? url-as-text?))
             conj
             lystros))

(defn map->vec-of-val+key
  "Convert a map of maps into a vector of maps that include the original key as a value
  e.g.:
  (map->vec-of-val+key {:x1 {:a 1} :x2 {:a 2} :x3 {:a 3}} :id)
  => [{:a 1 :id :x1} {:a 2 :id :x2} {:a 3 :id :x3}]
  "
  [vals-map key-key]
  (reduce-kv
   (fn [coll k v]
     (conj coll (assoc v key-key k)))
   []
   vals-map))

(defn- lystros-with-id [lystros-tree]
  (map->vec-of-val+key lystros-tree :firebase-id))

(defn- setify-tags [lystros]
  (reduce-kv (fn [m k v]
               (assoc m k (update v :tags set)))
             {} lystros))

(defn cleanup-lystros
  "Convert group of Lystros from internal Firebase format to proper form"
  [raw-lystros]
  (-> raw-lystros
      setify-tags
      lystros-with-id))


(re-frame/reg-sub
 ::lystros
 ;; [TODO] Got rid of shortcut syntax here, 26Nov17, because with it the private
 ;;        lystros were often not showing. I don't understand why, but this seems
 ;;        to fix it.
 ;; - :<- [:firebase/on-value {:path (all-shared-fb-path [:lystros])}]
 ;; - :<- [:firebase/on-value {:path (private-fb-path [:lystros])}]
 ;; + (fn [_ _]
 ;; +   [(re-frame/subscribe [:firebase/on-value {:path (all-shared-fb-path [:lystros])}])
 ;; +    (re-frame/subscribe [:firebase/on-value {:path (private-fb-path [:lystros])}])])
 (fn [_ _]
   [(re-frame/subscribe [:firebase/on-value {:path (all-shared-fb-path [:lystros])}])
    (re-frame/subscribe [:firebase/on-value {:path (private-fb-path [:lystros])}])])
 (fn [[shared-lystros private-lystros] [_ {:keys [tags-mode tags url text tags-as-text? url-as-text?] :as options}] _]
   (into (filter-lystros (cleanup-lystros private-lystros) options)
         (mapcat #(filter-lystros (cleanup-lystros %) options)
                 (vals shared-lystros)))))

(re-frame/reg-sub
 ::all-tags
 :<- [::lystros]
 (fn [lystros]
   (into #{} (mapcat :tags lystros))))


(re-frame/reg-sub
 ::tag-counts
 :<- [::lystros]
 (fn [lystros]
   (frequencies (mapcat :tags lystros))))

(re-frame/reg-sub
 ::max-tag-count
 :<- [::tag-counts]
 (fn [counts _]
   (apply max (vals counts))))

(re-frame/reg-sub
 ::tag-class-by-frequency
 :<- [::tag-counts]
 :<- [::max-tag-count]
 (fn [[counts max-count] [_ tag]]
   (let [count (counts tag)]
     (cond (<= count 1) "rare-tag"
           (<= count (/ max-count 2)) "average-tag"
           :else "common-tag"))))


(re-frame/reg-sub
 ::new-tags
 (fn [_ [_ tags]]
   (let [old-tags (<sub [::all-tags])]
     (clojure.set/difference (set tags) (set old-tags)))))



(defn normalize-url [url]
  (if (string? url)
    (uri/parse url)
    url))

(defn url= [url1 url2]
  (let [url1 (normalize-url url1)
        url2 (normalize-url url2)]
    (and (= (:host url1) (:host url2))
         (= (:path url1) (:path url2)))))


;; [TODO] Really, I think, this should filter within Firebase, rather than retrieving
;; all. But I don't know the syntax yet, and this might be premature optimization, so
;; let's leave well enough alone for now.
(re-frame/reg-sub
 ::lystros-of-url
 :<- [::lystros]
 (fn [lystros [_ url]]
   ;; [TODO] Keep this logging here until I fix spurious zero counts
   (console :log "::LYSTROS-OF-URL:" (count lystros) url)
   (filter #(and url (url= (:url %) url))
           lystros)))

