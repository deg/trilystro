;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.db
  (:require
   [trilystro.fsm :as fsm]
   [trilystro.modal :as modal]))

(def default-db
  {:name "Trilystro"
   ::modal/all-modals #{}
   ::fsm/page-states {:start                {:initialize-db        [:shift :logged-out]}
                      :logged-out           {:login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]
                                             :try-login            [:shift :logging-in]}
                      :logging-in           {:firebase-error       [:shift :error]
                                             :login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]}
                      :logged-in            {:login-confirmed      [:shift :logged-in]
                                             :logout               [:shift :logged-out]}
                      :error                {:error-handled        [:shift :start]}}
   ::fsm/page-state [[:start]]})
