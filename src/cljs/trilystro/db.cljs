;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.db
  (:require
   [trilystro.fsm :as fsm]))

(def default-db
  {:name "Trilystro"
   ::fsm/page-states {:start                {:initialize-db        [:shift :logged-out]}
                      :logged-out           {:login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]
                                             :try-login            [:shift :logging-in]
                                             :modal-about          [:push :modal-about]}
                      :logging-in           {:firebase-error       [:shift :error]
                                             :login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]}
                      :logged-in            {:login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]
                                             :modal-confirm-delete [:push :modal-confirm-delete]
                                             :modal-edit-lystro    [:push :modal-edit-lystro]
                                             :modal-new-lystro     [:push :modal-new-lystro]
                                             :modal-show-exports   [:push :modal-show-exports]
                                             :modal-about          [:push :modal-about]}
                      :modal-edit-lystro    {:quit-modal           [:pop]}
                      :modal-new-lystro     {:quit-modal           [:pop]}
                      :modal-about          {:quit-modal           [:pop]}
                      :modal-confirm-delete {:quit-modal           [:pop]}
                      :modal-show-exports   {:quit-modal           [:pop]}
                      :error                {:error-handled        [:shift :start]}}
   ::fsm/page-state [[:start]]})
