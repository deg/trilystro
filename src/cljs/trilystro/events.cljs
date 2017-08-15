(ns trilystro.events
  (:require [firebase-cljs.core :as fb]
            [firebase-cljs.auth :as fb.auth]
            [firebase-cljs.database :as fb.database]
            [firebase-cljs.auth.provider :as fb.auth.provider]
            [re-frame.core :as re-frame]
            [trilystro.db :as db]))

(defonce fb-init
  (do
    (fb/init
     {:apiKey "" ;; [TOOD] Learn if this is safe to commit. For now, don't.
      :authDomain "trilystro.firebaseapp.com"
      :databaseURL "https://trilystro.firebaseio.com"
      :storageBucket "trilystro.appspot.com"})
    (fb.auth/login-popup
     (fb/get-auth)
     (fb.auth.provider/google))))

;;; **************** TOY CODE, FOR NOW ****************
(defonce db (fb/get-db))
(fb.database/reset! db {:it "works"})
(fb.database/remove! db)


(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
