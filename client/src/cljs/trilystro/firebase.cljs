;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.firebase
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [iron.re-utils :as re-utils :refer [sub2 <sub]]
   [com.degel.re-frame-firebase :as firebase]
   [trilystro.db :as db]
   [trilystro.fsm :as fsm]))


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
  (re-frame/dispatch-sync [::fsm/goto :try-login])
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


(re-frame/reg-event-fx
 ::set-user
 [db/check-spec-interceptor]
 (fn [{db :db} [_ user]]
   (into {:db (assoc db ::user user)
          :dispatch [::fsm/goto (if user :login-confirmed :logout)]}
         (when user
           ;; [TODO][ch94] Should remain :user-details to :users-details in Firebase.
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






(comment
  (re-frame/reg-event-fx
   :fb-test
   [db/check-spec-interceptor]
   (fn [_ [_ & {:keys [event-type path value on-success on-failure]}]]
     {event-type {:path path
                  :value value
                  :on-success on-success
                  :on-failure on-failure}}))

  (re-frame/reg-event-db
   :got-read
   [db/check-spec-interceptor]
   (fn [_ [_ val]] (prn "READ: " val)))

  (re-utils/>evt [:fb-test
                  :event-type :firebase/write
                  :path [:public :new]
                  :value 123
                  :on-success #(prn "GOOD")
                  :on-failure #(prn "BAD" %)])

  (re-utils/>evt [:fb-test
                  :event-type :firebase/write
                  :path [:not-public]
                  :value 123
                  :on-success #(prn "GOOD")
                  :on-failure #(prn "BAD" %)])

  (re-utils/>evt [:fb-test
                  :event-type :firebase/read-once
                  :path [:public :new]
                  :on-success [:got-read]
                  :on-failure #(prn "BAD" %)])
  )
