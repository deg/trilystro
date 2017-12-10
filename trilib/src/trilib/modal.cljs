;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilib.modal
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [iron.re-utils :refer [<sub >evt]]
   [trilib.fsm-lib :as fsm-lib]
   [trilib.fsm :as fsm]
   [trilib.fsm-graph :as fsm-graph]))


(s/def ::all-modal-views (s/coll-of fn?))
(s/def ::db-keys (s/keys :req [::all-modal-views]))

(re-frame/reg-sub
 ::all-modal-views
 (fn [db _]
   (mapv vector (::all-modal-views db))))

(defn register-modal [db [from-states modal view]]
  (as-> db $
    (update $ ::all-modal-views conj view)
    (reduce #(update %1 ::fsm/page-graph fsm-lib/add-transition
                     %2 modal [:push modal])
            $ from-states)
    (update $ ::fsm/page-graph fsm-lib/add-transition
            modal :quit-modal [:pop])))

(re-frame/reg-event-db
 ::register-modal
 ;;[db/check-spec-interceptor]  ;; [TODO] Untangle deps, so we can call this here
 (fn [db [_ from-states modal view]]
   (register-modal db [from-states modal view])))

(re-frame/reg-event-db
 ::register-modals
 ;;[db/check-spec-interceptor] ;; [TODO] Untangle deps, so we can call this here
 (fn [db [_ modals]]
   (reduce register-modal db modals)))


(defn goto
  ;; Enter a modal popup, optionally passing it state
  ([page]
   (goto page nil))
  ([page param]
   [::fsm/goto page {:param param}]))

(defn quit []
  (>evt [::fsm/goto :quit-modal]))


(defn modal [{:keys [page open? header]} content]
  (let [open? (if page
                (<sub [::fsm/in-page? page])
                open?)]
    [na/modal {:open? open?
               :dimmer "blurring"
               :close-icon true
               :close-on-dimmer-click? true
               :on-close #(quit)}
     [na/modal-header {} header]
     [na/modal-content {}
      (when open?
        content)]]))
