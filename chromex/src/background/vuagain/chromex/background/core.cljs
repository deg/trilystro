(ns vuagain.chromex.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
   [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
   [chromex.ext.runtime :as runtime]
   [chromex.ext.tabs :as tabs]
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [chromex.protocols :refer [post-message! get-sender]]
   [cljs.core.async :refer [<! chan]]
   [com.degel.re-frame-firebase :as firebase]
   [com.degel.re-frame-firebase.auth :as auth]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [trilib.firebase :as fb]))


;;; All connected clients (popup or content script)
(defonce clients (atom []))

;;; -- Firebase authentication ----------------

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
                 :get-user-sub           [::fb/user]
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
  {:command "got-user"
   :user (<sub [::fb/user])})


(defn set-user [user]
  (re-frame/dispatch-sync [::fb/poor-mans-set-user user])

  ;; [TODO] This is pretty hokey.
  ;; We are calling the subscription here to persuade Firebase to load it into memory.
  ;; But, I don't really understand the rules, and I suspect this will be much cleaner
  ;; if we talk to Firebase without going through the re-frame hoops. So, fix someday,
  ;; presumably when we get both the client and the popup to use the background process
  ;; for all Firebase access.  (?? But, how does that play on mobile and other platforms
  ;; besides Chrome Desktop).
  (when user (<sub [::fb/lystros]))

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
  "Process a VuAgain request from popup or content script"
  [client message]
  (let [client-url (-> client get-sender js->clj (get "url"))]
    (if (and (map? message)
             (= (:app message) "VuAgain"))
      (case (:command message)
        "sign-in"  (google-sign-in)
        "sign-out" (sign-out)
        "user"     (msg->client client (user-message))
        "check-url" (msg->client
                     client
                     {:command "checked-url"
                      :callback-params (:callback-params message)
                      :lystros (<sub [::fb/lystros-of-url (:url message)])})
        (error "Unhandled" (:command message) "message from" client-url": " message))
      (console :error
               "Received message from "
               client-url
               "in unknown format: "
               message))))


(defn run-client-message-loop!
  "Create async channel to receive messages from a client"
  [client]
  (add-client! client)
  (go-loop []
    (when-some [message (<! client)]
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
