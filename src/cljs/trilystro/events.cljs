;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.events
  (:require
   [cljs-time.coerce :as time-coerce]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.chrome-utils :as chrome]
   [sodium.re-utils :as re-utils :refer [<sub]]
   [trilystro.db :as db]
   [com.degel.re-frame-firebase :as firebase]))

(s/check-asserts true)

(defn private-fb-path
  ([path]
   (private-fb-path path nil))
  ([path for-uid]
   (if-let [uid (or for-uid
                    (<sub [:uid]))]
     (into [:private uid] path))))

(defn public-fb-path [path]
  (if-let [uid (<sub [:uid])]
    (into [:public] path)))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :page
 (fn [db [_ page]]
   (assoc db :page page)))

(re-frame/reg-event-db
 :form-state
 (fn [db [_ form-name form-component value]]
   (if form-component
     (assoc-in db `[:forms ~form-name ~@form-component] value)
     (assoc-in db `[:forms ~form-name] value))))

(re-frame/reg-event-fx
 :set-user
 (fn [{db :db} [_ user]]
   (into {:db (assoc db :user user)}
         (when user
           {:firebase/write {:path       (private-fb-path [:user-details] (:uid user))
                             :value      (select-keys user [:display-name :email :photo-url])
                             :on-success #(console :log "Logged in:" (:display-name user))
                             :on-failure #(console :error "Failure: " %)}}))))

(re-frame/reg-event-fx
 :sign-in
 (fn [_ _] {:firebase/google-sign-in nil}))

(re-frame/reg-event-fx
 :sign-out
 (fn [_ _] {:firebase/sign-out nil}))

(re-frame/reg-event-fx
 :firebase-error
 (fn [_ [_ error]]
   (js/console.error (str "FIREBASE ERROR:\n" error))))


(re-frame/reg-event-fx
 :db-write-public
 (fn [{db :db} [_ {:keys [path value on-success on-failure] :as args}]]
   (if-let [path (public-fb-path path)]
     {:firebase/write (assoc args :path path)}

     ;; [TODO] Need to use pending Sodium generalization of :dispatch that takes a fn too.
     ((if on-failure (re-utils/event->fn on-failure) js/alert)
      (str "Can't write to Firebase, because not logged in:/n " path ": " value)))))


(defn logged-in? [db]
  (some? (get-in db [:user :uid])))


(defn fb-event [{:keys [db path value on-success on-failure public? effect-type for-multi?] :as args}]
  (if (logged-in? db)
    (let [path ((if public? public-fb-path private-fb-path) path)
          effect-args (assoc (select-keys args [:value :on-success :on-failure])
                             :path path)]
      (if for-multi?
        [effect-type effect-args]
        {effect-type effect-args}))

    ;; [TODO] Need to use pending Sodium generalization of :dispatch that takes a fn too.
    ((if on-failure
       (re-utils/event->fn on-failure)
       js/alert)
     (str "Can't write to Firebase, because not logged in:/n " path ": " value))))


(defn new-tags [tags]
  (let [old-tags (vals (<sub [:firebase/on-value {:path [:public :tags]}]))]
    (into [] (clojure.set/difference (set tags) (set old-tags)))))

(re-frame/reg-event-fx
 :commit-lystro
 (fn [{db :db} [_ form-key]]
   (let [form-path [:forms form-key]
         form-vals (get-in db form-path)
         {:keys [tags url text]} form-vals]
     {:firebase/multi (conj (mapv #(fb-event {:for-multi? true
                                              :effect-type :firebase/push
                                              :db db
                                              :public? true
                                              :path [:tags]
                                              :value %})
                                  (new-tags tags))
                            (let [options {:for-multi? true
                                           :db db
                                           :public? false
                                           :value {:tags tags :url url :text text}} ]
                              (if-let [old-id (:firebase-id form-vals)]
                                (fb-event (assoc options
                                                 :effect-type :firebase/write
                                                 :path [:items old-id]))
                                (fb-event (assoc options
                                                 :effect-type :firebase/push
                                                 :path [:items])))))
      :db (assoc-in db form-path nil)})))

(re-frame/reg-event-fx
 :clear-lystro
 (fn [{db :db} [_ id]]
   (fb-event {:for-multi? false
              :effect-type :firebase/write
              :db db
              :public? false
              :path [:items id]
              :value nil})))


(defn set-conj [set new]
  (conj (or set #{}) new))

(re-frame/reg-event-db
 :add-new-tag
 (fn [db [_ form-key]]
   (let [form-path [:forms form-key]
         new-tag (get-in db (conj form-path :new-tag))]
     (-> db
         (update    :new-tags                  set-conj new-tag)
         (update-in (conj form-path :tags)     set-conj new-tag)
         (assoc-in  (conj form-path :new-tag)  "")))))




(comment
  (re-frame/reg-event-fx
   :fb-test
   (fn [_ [_ & {:keys [event-type path value on-success on-failure]}]]
     {event-type {:path path
                  :value value
                  :on-success on-success
                  :on-failure on-failure}}))

  (re-frame/reg-event-db
   :got-read
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
