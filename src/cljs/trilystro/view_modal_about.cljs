;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-about
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm :as fsm]
   [trilystro.fsm-graph :as fsm-graph]
   [trilystro.modal :as modal]))


(defn view-modal-about []
  [modal/modal {:page :modal-about
                :header (str "About " (<sub [:name]))}
   [na/container {}
    [:div "Trilystro is still a toy app, playing with ideas about Firebase and data curation."]
    [:div {:class "credits"} "Copyright (c) 2017, David Goldfarb (deg@degel.com)"]
    [:div {:class "credits"}
     (let [{:keys [commit date]} (<sub [:git-commit])]
       (str "This version built from GIT commit: " commit " of " date))]
    (fsm-graph/render-graph (<sub [::fsm/page-states]))]])
