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
            [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
            [trilib.browser-utils :refer [dev-setup]]
            [trilib.firebase :as fb]
            [trilib.fsm :as fsm]
            [vuagain.chromex.popup.db :as db]))


(defn process-message! [message]
  (log "POPUP: got message:" message)
  (when (map? (js->clj message))
    (>evt [::fb/set-user (:user (js->clj message :keywordize-keys true))])))

(defn run-message-loop! [message-channel]
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


(re-frame/reg-event-fx
 ::initialize-db
 (fn  [_ _]
   {:db (fsm/goto db/default-db :initialize-db {})
    ;; :http-xhrio {:method :get
    ;;              :uri "/git-describe.txt"
    ;;              :params {:cachebuster (str (rand))}
    ;;              :response-format (ajax/text-response-format)
    ;;              :on-success [:got-git-describe]
    ;;              :on-failure [:no-git-describe]}
    ;; :dispatch [::modal/register-modals
    ;;            [[[:logged-in
    ;;               :logged-out] :modal-about          v-about/view-modal-about]
    ;;             [[:logged-in]  :modal-confirm-delete v-confirm-delete/view-modal-confirm-delete]
    ;;             [[:logged-in]  :modal-edit-lystro    v-entry/view-modal-entry-panel]
    ;;             [[:logged-in]  :modal-new-lystro     v-entry/view-modal-entry-panel]
    ;;             [[:logged-in]  :modal-show-exports   v-show-exports/view-modal-show-exports]]]
    }))


;;; -- main entry point ----------------
(defn mount-root []
  (reagent/render [views/popup]
                  (.getElementById js/document "app")))

(defn init! []
  (re-frame/dispatch-sync [::initialize-db])
  (dev-setup)
  (>evt [:set-background-port (connect-to-background-page!)])
  (fb/init)
  (set-current-tab)
  (mount-root))
