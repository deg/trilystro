;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.modal
  (:require
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm-lib :as fsm-lib]
   [trilystro.fsm :as fsm]
   [trilystro.fsm-graph :as fsm-graph]))


(re-frame/reg-sub
 ::all-modal-views
 (fn [db _]
   (mapv vector (::all-modal-views db))))

(re-frame/reg-event-db
 ::register-modal
 (fn [db [_ from-states modal view]]
   (as-> db $
     (update $ ::all-modal-views conj view)
     (reduce #(update %1 ::fsm/page-states fsm-lib/add-transition
                      %2 modal [:push modal])
             $ from-states)
     (update $ ::fsm/page-states fsm-lib/add-transition
             modal :quit-modal [:pop]))))

(defn goto
  ;; Enter a modal popup, optionally passing it state
  ([page]
   (goto page nil))
  ([page param]
   [::fsm/goto page {:param param}]))

(defn quit []
  (na/>event [::fsm/goto :quit-modal]))


(defn modal [{:keys [page open? header]} content]
  (let [open? (if page
                (<sub [::fsm/in-page? page])
                open?)]
    [na/modal {:open? open?
               :dimmer "blurring"
               :close-icon true
               :close-on-dimmer-click? true
               :on-close (quit)}
     [na/modal-header {} header]
     [na/modal-content {}
      (when open?
        content)]]))
