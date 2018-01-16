;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.core
  (:require
   [ajax.core :as ajax]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [re-frisk.core :refer [enable-re-frisk!]]
   [reagent.core :as reagent]
   [trilib.browser-utils :refer [dev-setup]]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]
   [trilib.modal :as modal]
   [trilystro.db :as db]
   [trilystro.events]
   [trilystro.subs]
   [trilystro.view-modal-about :as v-about]
   [trilystro.view-modal-confirm-delete :as v-confirm-delete]
   [trilystro.view-modal-entry :as v-entry]
   [trilystro.view-modal-show-exports :as v-show-exports]
   [trilystro.views :as views]))


(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app-view]
                  (.getElementById js/document "app")))

(re-frame/reg-event-fx
 ::initialize-db
 (fn  [_ _]
   {:db (fsm/goto db/default-db :initialize-db {})
    :http-xhrio {:method :get
                 :uri "/git-describe.txt"
                 :params {:cachebuster (str (rand))}
                 :response-format (ajax/text-response-format)
                 :on-success [:got-git-describe]
                 :on-failure [:no-git-describe]}
    :dispatch [::modal/register-modals
               [[[:logged-in
                  :logged-out] :modal-about          v-about/view-modal-about]
                [[:logged-in]  :modal-confirm-delete v-confirm-delete/view-modal-confirm-delete]
                [[:logged-in]  :modal-edit-lystro    v-entry/view-modal-entry-panel]
                [[:logged-in]  :modal-new-lystro     v-entry/view-modal-entry-panel]
                [[:logged-in]  :modal-show-exports   v-show-exports/view-modal-show-exports]]]}))


(defn ^:export init []
  (re-frame/dispatch-sync [::initialize-db])
  (dev-setup)
  (fb/init :sandbox :production)
  (mount-root))


;;; This project assumes corresponding Firebase auth rules:
;;; See firebase-rules.json in the root of trilib of this project.
