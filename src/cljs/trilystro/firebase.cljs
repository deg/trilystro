;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.firebase
  (:require
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.re-utils :as re-utils :refer [<sub]]
   [com.degel.re-frame-firebase :as firebase]
   [trilystro.fsm :as fsm]))


;;; From https://console.firebase.google.com/u/0/project/trilystro/overview - "Add Firebase to your web app"
(defonce firebase-app-info
  {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
   :authDomain "trilystro.firebaseapp.com"
   :databaseURL "https://trilystro.firebaseio.com"
   :storageBucket "trilystro.appspot.com"})


(defn init []
  (re-frame/dispatch-sync [::fsm/goto :try-login])
  (firebase/init :firebase-app-info      firebase-app-info
                 :get-user-sub           [:user]
                 :set-user-event         [:set-user]
                 :default-error-handler  [:firebase-error]))


(defn private-fb-path
  ([path]
   (private-fb-path path (<sub [:uid])))
  ([path uid]
   (when uid
     (into [:private uid] path))))

(defn all-shared-fb-path
  [path]
  (into [:shared] path))


(defn my-shared-fb-path
  ([path]
   (my-shared-fb-path path (<sub [:uid])))
  ([path uid]
   (when uid
     (into [:shared (first path) uid] (rest path)))))

(defn public-fb-path [path]
  (if-let [uid (<sub [:uid])]
    (into [:public] path)))

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
      (str "Can't write to Firebase, because not logged in:\n " path ": " value)))))

(defn logged-in? [db]
  (some? (get-in db [:user :uid])))

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

    ;; [TODO] Need to use pending Sodium generalization of :dispatch that takes a fn too.
    ((if on-failure
       (re-utils/event->fn on-failure)
       js/alert)
     (str "Can't write to Firebase, because not logged in:\n " path ": " value))))

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
