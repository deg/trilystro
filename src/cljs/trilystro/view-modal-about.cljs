;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.views-modal-about
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm :as fsm]
   [trilystro.fsm-graph :as fsm-graph]))


(defn about-panel []
  [na/container {}
   [:div "Trilystro is still a toy app, playing with ideas about Firebase and data curation."]
   [:div "Copyright (c) 2017, David Goldfarb (deg@degel.com)"]
   (fsm-graph/render-graph (<sub [::fsm/page-states]))])

(defn modal-about-panel []
  [na/modal {:open? (<sub [::fsm/in-page? :modal-about])
             :dimmer "blurring"
             :close-icon true
             :close-on-dimmer-click? false
             :on-close (na/>event [::fsm/goto :quit-modal])}
   [na/modal-header {}
    (str "About " (<sub [:name]))]
   [na/modal-content {}
    [about-panel]]])

