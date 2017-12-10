(ns vuagain.chromex.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [com.degel.re-frame-firebase :as firebase]
            [com.degel.re-frame-firebase.auth :as auth]))


;;; All connected clients (popup or content script)
(defonce clients (atom []))

;;; -- Firebase authentication ----------------

;;; User, as identified by Firebase
(defonce logged-in-user (atom nil))

(defonce firebase-app-info
  {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
   :authDomain "trilystro.firebaseapp.com"
   :databaseURL "https://trilystro.firebaseio.com"
   :storageBucket "trilystro.appspot.com"})

(defn firebase-error [err]
  (error (str "FIREBASE ERROR:\n" err)))

(declare set-user)

(defn init-fb
  "Initialize Firebase interface"
  []
  (firebase/init :firebase-app-info      firebase-app-info
                 :get-user-sub           (fn [] @logged-in-user)
                 :set-user-event         set-user
                 :default-error-handler  firebase-error))


(defn google-sign-in
  "Login to Firebase, using Google authentication"
  []
  (auth/google-sign-in {:sign-in-method :popup}))


(defn sign-out
  "Logout from Firebase"
  []
  (auth/sign-out))


;;; -- client event loop ----------------

(defn msg->client [client message]
  (post-message! client (clj->js (assoc message :app "VuAgain"))))

(defn msg->all-clients [message]
  (doseq [client @clients]
    (msg->client client message)))


(defn user-message []
  {:user @logged-in-user})


(defn set-user [user]
  (reset! logged-in-user user)
  (msg->all-clients (user-message)))


(defn client-url
  "(Mostly for debuging now)"
  [client]
  (-> client get-sender (get "url")))

(defn add-client! [client]
  (swap! clients conj client)
  (log "**************** BACKGROUND: client connected" (count @clients) (client-url client)))


(defn remove-client! [client]
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client))
  (log "**************** BACKGROUND: client disconnected" (count @clients) (client-url client)))


(defn dispatch-message
  "Send a VuAgain message"
  [this-client message]
  (when (and (map? message)
             (= (:app message) "VuAgain"))
    (log "CLIENT: " this-client)
    (case (:command message)
      "sign-in"  (google-sign-in)
      "sign-out" (sign-out)
      "user"     (msg->client this-client (user-message))
      (error "Unknown message:" (:command message)))))


(defn run-client-message-loop!
  "Create async channel to receive messages from a client"
  [client]
  (log "BACKGROUND: starting event loop for client:" (get-sender client))
  (add-client! client)
  (go-loop []
    (when-some [message (<! client)]
      (log "BACKGROUND: got client message:" message #_"from" #_(get-sender client))
      (dispatch-message client (js->clj message :keywordize-keys true))
      (recur))
    (log "BACKGROUND: leaving event loop for client:" (get-sender client))
    (remove-client! client)))



;;; -- event handlers ----------------

(defn handle-client-connection!
  "Respond to client connection request"
  [client]
  (run-client-message-loop! client)
  (msg->client client (user-message)))


(defn process-chrome-event
  "Process one received event"
  [event-num event]
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      nil #_(log "Unhandled event: " event))))


(defn run-chrome-event-loop!
  "Listen for events from Chrome"
  [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (init-fb)
  (go-loop [event-num 1]
    (when-some [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))
    (log "BACKGROUND: leaving main event loop")))


(defn init!
  "Start the background page"
  []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))
