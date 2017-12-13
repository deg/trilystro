(ns trilib.db
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [re-frame.core :as re-frame :refer [after]]
   [re-frame.loggers :refer [console]]
   [iron.closure-utils :refer [debug?]]
   [trilib.modal]))

;;; (See https://github.com/Day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs)
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when (and debug?
             (not (s/valid? a-spec db)))
    (throw (ex-info (binding [s/*explain-out* expound/printer]
                      (s/explain a-spec db))
                    {}))))

(s/def ::db  (s/merge :trilib.firebase/db-keys   ;; [TODO] Still needs cleanup
                      :trilib.modal/db-keys
                      :trilib.fsm/db-keys))

(def default-db
  {:trilib.firebase/user nil
   :trilib.modal/all-modal-views #{}
   :trilib.fsm/page-graph
   {:start                {:initialize-db        [:shift :logged-out]}
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
   :trilib.fsm/page-state-stack [[:start nil]]})


(def check-spec-interceptor (after (partial check-and-throw ::db)))
