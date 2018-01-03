(ns vuagain.chromex.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [re-frame.loggers :refer [console]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [hipo.core :as hipo]
            [oops.core :as oops]))


;; [TODO] Create utility that wraps post-message! and clj->js. Expose both here and in popup



(defn match-vuid [doms vuid]
  (first (filter #(= vuid (oops/oget % "dataset.vuid")) doms)))

(defn process-message! [message]
  (if (and (map? message)
           (= (:app message) "VuAgain"))
    (case (:command message)
      "got-user" identity ;(console :log "USER is: " (:user message))
      "checked-url" (let [{:keys [callback-params lystros]} message
                          vuid (:vuid callback-params)
                          sections (sel ".rc")
                          section (match-vuid sections vuid)]
                      (when-not (empty? lystros)
                        (dommy/append! section (hipo/create [:div {:style "color:brown; background-color:burlyWood;"}
                                                             "VuAgain: " (str lystros)]))))
      (error "Unhandled" (:command message) "message from background: " message) )
    (console :error "Received message from background in unknown format: " message)))

(defn run-message-loop! [message-channel]
  ;;(log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! (js->clj message :keywordize-keys true))
      (recur))
    ;;(log "CONTENT SCRIPT: leaving message loop")
    ))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn do-page-analysis! [background-port]
  (let []
    (run! (fn [section]
            (let [url (.-href (sel1 section "h3 a"))
                  vuid (str (rand-int 999999999))]
              (oops/oset! section "dataset.vuid" vuid)
              (post-message! background-port (clj->js {:app "VuAgain"
                                                       :command :check-url
                                                       :url url
                                                       :callback-params {:vuid vuid}}))))
          (sel ".rc"))))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)
    (do-page-analysis! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!))
