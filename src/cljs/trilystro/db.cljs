;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.db
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [re-frame.core :as re-frame :refer [after]]
   [re-frame.loggers :refer [console]]
   [trilystro.config :as config]))


(def default-db
  {::name "Trilystro"
   :trilystro.firebase/user nil
   :trilystro.fsm/page-graph {:start                {:initialize-db        [:shift :logged-out]}
                              :logged-out           {:login-confirmed      [:shift :logged-in]
                                                     :logout               [:shift :logged-out]
                                                     :try-login            [:shift :logging-in]}
                              :logging-in           {:firebase-error       [:shift :error]
                                                     :login-confirmed      [:shift :logged-in]
                                                     :logout               [:shift :logged-out]}
                              :logged-in            {:login-confirmed      [:shift :logged-in]
                                                     :logout               [:shift :logged-out]}
                              ;; [TODO] Eventually figure out what an error handler can do
                              :error                {:error-handled        [:shift :error]}}
   :trilystro.fsm/page-state-stack [[:start]]
   :trilystro.modal/all-modal-views #{}})


(s/def ::name string?)

(s/def ::commit string?)
(s/def ::date string?)  ;; [TODO] Needs to be namespaced! Ditto for rest of git and user
(s/def ::git-commit (s/keys :req-un [::commit ::date]))

(s/def ::db (s/merge :trilystro.firebase/db-keys
                     :trilystro.fsm/db-keys
                     :trilystro.modal/db-keys
                     (s/keys :req [::name] :opt-un [::git-commit])))

;;; (See https://github.com/Day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs)
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when (and config/debug?
             (not (s/valid? a-spec db)))
    (throw (ex-info (binding [s/*explain-out* expound/printer]
                      (s/explain a-spec db))
                    {}))))

(def check-spec-interceptor (after (partial check-and-throw ::db)))
