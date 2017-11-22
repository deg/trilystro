(ns vuagain.chromex.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [vuagain.chromex.background.storage :refer [test-storage!]]

            [com.degel.re-frame-firebase :as firebase]
            [re-frame.core :as re-frame]
            [sodium.re-utils :as re-utils :refer [sub2 <sub >evt]]))

;; -- TEMP TEST ----------------------------------------------------------------

(defonce firebase-app-info
  {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
   :authDomain "trilystro.firebaseapp.com"
   :databaseURL "https://trilystro.firebaseio.com"
   :storageBucket "trilystro.appspot.com"})

(defn init-fb []
  (firebase/init :firebase-app-info      firebase-app-info
                 :get-user-sub           [::user]
                 :set-user-event         [::set-user]
                 :default-error-handler  [:firebase-error]))

(sub2 ::user [::user])
(sub2 ::uid  [::user :uid])

(re-frame/reg-event-fx
 :firebase-error
 (fn [_ [_ error]]
   (js/console.error (str "FIREBASE ERROR:\n" error))))

(re-frame/reg-event-fx
 ::set-user
 (fn [{db :db} [_ user]]
   (js/console.log (str "DB: " db))
   (js/console.log (str "SET-USER: " user))
   {:db (assoc db ::user user)}))

(re-frame/reg-event-fx
 :sign-in
 (fn [_ _]
   (js/console.log "TRYING SIGN-IN")
   {:firebase/google-sign-in {:sign-in-method :popup}}))





(def clients (atom []))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (log "BACKGROUND: client connected" (get-sender client))
  (swap! clients conj client))

(defn remove-client! [client]
  (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn run-client-message-loop! [client]
;;  (log "BACKGROUND: starting event loop for client:" (get-sender client))
  (go-loop []
    (when-some [message (<! client)]
;;      (log "BACKGROUND: got client message:" message "from" (get-sender client))
      (recur))
;;    (log "BACKGROUND: leaving event loop for client:" (get-sender client))
    (remove-client! client)))

; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (post-message! client "hello from BACKGROUND PAGE!")
  (run-client-message-loop! client))

(defn tell-clients-about-new-tab! []
  (doseq [client @clients]
    (post-message! client "a new tab was created")))

; -- main event loop --------------------------------------------------------------------------------------------------------

(defn process-chrome-event [event-num event]
  ;(log (gstring/format "BACKGROUND: got chrome event (%05d)" event-num) event)
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::tabs/on-created (tell-clients-about-new-tab!)
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (init-fb)
  (>evt [:sign-in])
  (go-loop [event-num 1]
    (when-some [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))
    (log "BACKGROUND: leaving main event loop")))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "BACKGROUND: init")
  (test-storage!)
  (boot-chrome-event-loop!))
