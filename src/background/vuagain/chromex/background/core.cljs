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
            [re-frame.loggers :refer [console]]
            [vuagain.chromex.background.storage :refer [test-storage!]]

            [com.degel.re-frame-firebase :as firebase]
            [com.degel.re-frame-firebase.auth :as auth]
            [re-frame.core :as re-frame]
            [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]))

(defonce clients (atom []))

;; -- TEMP TEST ----------------------------------------------------------------

(defonce firebase-app-info
  {:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
   :authDomain "trilystro.firebaseapp.com"
   :databaseURL "https://trilystro.firebaseio.com"
   :storageBucket "trilystro.appspot.com"})

(defonce logged-in-user (atom nil))

(defn js->cljk [x]
  (js->clj x :keywordize-keys true))

(defn client-url [client]
  (-> client get-sender js->cljk :url))

(defn set-user [user]
  (let [old @logged-in-user]
    (reset! logged-in-user user)
    (log "User was " old "; now " (:display-name user))
    (doseq [client @clients]
      (log "POSTING USER TO: " (client-url client))
      (post-message! client (clj->js {:user @logged-in-user})))))

(defn firebase-error [error]
  (js/console.error (str "FIREBASE ERROR:\n" error)))


(defn init-fb []
  (firebase/init :firebase-app-info      firebase-app-info
                 :get-user-sub           (fn [] @logged-in-user)
                 :set-user-event         set-user
                 :default-error-handler  firebase-error))


(defn google-sign-in []
  (js/console.log "TRYING SIGN-IN")
  (auth/google-sign-in {:sign-in-method :popup}))

(defn sign-out []
  (js/console.log "TRYING SIGN-OUT")
  (auth/sign-out))



; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (swap! clients conj client)
  (log "**************** BACKGROUND: client connected" (count @clients) (client-url client)))

(defn remove-client! [client]
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client))
  (log "**************** BACKGROUND: client disconnected" (count @clients) (client-url client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn msg->popup [client message]
  (post-message! client (clj->js (assoc message :app "VuAgain"))))


(defn dispatch-message [this-client message]
  (when (and (map? message)
             (= (:app message) "VuAgain"))
    (log "CLIENT: " this-client)
    (case (:command message)
      "sign-in"  (google-sign-in)
      "sign-out" (sign-out)
      "user"    (do
                  (log "Client: " this-client)
                  (log "IS: " (client-url this-client))
                  (log "Clients: " @clients)
                  (log "ARE: " (map client-url @clients))
                  (log "Count?" (count @clients))
                  (log "EQ?" (= this-client (first @clients)))
                  (log "MAP?" (map? this-client) (map? (first @clients)))
                  (log "TYPE?" (type this-client) (type (first @clients)))
                  #_(doseq [client [this-client]]
                    (log "HAS: " (client-url this-client))
                    (post-message! client "Chopped Liver" #_@logged-in-user))
                  (doseq [client @clients]
                    (post-message! client (clj->js {:user @logged-in-user})))
                  )
      (error "Unknown message:" (:command message)))))

(defn run-client-message-loop! [client]
  #_(log "BACKGROUND: starting event loop for client:" (get-sender client))
  (go-loop []
    (when-some [message (<! client)]
      (log "BACKGROUND: got client message:" message #_"from" #_(get-sender client))
      (dispatch-message client (js->cljk message))
      (recur))
    (log "BACKGROUND: leaving event loop for client:" (get-sender client))
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
  ;;(log (gstring/format "BACKGROUND: got chrome event (%05d)" event-num) event)
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::tabs/on-created (tell-clients-about-new-tab!)
      nil #_(log "Unhandled event: " event))))

(defn run-chrome-event-loop! [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (init-fb)
  ;; (>evt [:sign-in])
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
