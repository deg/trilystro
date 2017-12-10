(ns vuagain.chromex.popup.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [vuagain.chromex.popup.handlers]
            [vuagain.chromex.popup.subs]
            [vuagain.chromex.popup.views :as views]
            [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]))

;;; -- a message loop ----------------

(defn process-message! [message]
  (log "POPUP: got message:" message)
  (when (map? (js->clj message))
    (>evt [:set-user (:user (js->clj message :keywordize-keys true))])))

(defn run-message-loop! [message-channel]
  (log "POPUP: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "POPUP: leaving message loop")))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "Hello from POPUP!")
    (run-message-loop! background-port)
    background-port))


(defn set-current-tab []
  "Get current tab info from Chrome and update into our app state"
  []
  (go
    (if-let [[[tab]] (<! (tabs/query #js {"active" true "currentWindow" true}))]
      (>evt [:set-current-tab] (js->clj tab :keywordize-keys true)))))


;;; -- main entry point ----------------
(defn mount-root []
  (reagent/render [views/popup]
                  (.getElementById js/document "app")))

(defn init! []
  (log "POPUP: init")
  (re-frame/dispatch-sync [:initialize-db])
  (>evt [:set-background-port (connect-to-background-page!)])
  (set-current-tab)
  (mount-root))
