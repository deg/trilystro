(ns trilystro.events
  (:require
   [cljs-time.coerce :as time-coerce]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [clojure.spec.alpha :as s]
   [firebase-cljs.auth :as fb.auth]
   [firebase-cljs.auth.provider :as fb.auth.provider]
   [firebase-cljs.core :as fb]
   [firebase-cljs.database :as fb.database]
   [re-frame.core :as re-frame]
   [trilystro.db :as db]))

(s/check-asserts true)

;;; From https://console.firebase.google.com/u/0/project/trilystro/overview - "Add Firebase to your web app"
;;; <script src="https://www.gstatic.com/firebasejs/4.2.0/firebase.js"></script>
;;; <script>
;;;   // Initialize Firebase
;;;   var config = {
;;;     apiKey: "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk",
;;;     authDomain: "trilystro.firebaseapp.com",
;;;     databaseURL: "https://trilystro.firebaseio.com",
;;;     projectId: "trilystro",
;;;     storageBucket: "trilystro.appspot.com",
;;;     messagingSenderId: "391423934969"
;;;   };
;;;   firebase.initializeApp(config);
;;; </script>
;;;

(defonce fb-init
  (do
    (fb/init
     {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
      :authDomain "trilystro.firebaseapp.com"
      :databaseURL "https://trilystro.firebaseio.com"
      :storageBucket "trilystro.appspot.com"})
    (fb.auth/login-popup
     (fb/get-auth)
     (fb.auth.provider/google))))

(defonce fb-db (fb/get-db))


(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(def time-format (time-format/formatters :date-time))

(re-frame/reg-event-db
 :db-write
 (fn [db [_ raw-datum]]
   (let [now (time-format/unparse time-format (time/now))
         datum {:time now :data raw-datum}]
     (prn "Will write: " datum)
     (fb.database/conj! fb-db (str datum)))
   db))
