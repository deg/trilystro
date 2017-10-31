;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-show-exports
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm :as fsm]))

(defn export-lystro [{:keys [firebase-id public? owner text url tags]}]
  {:uid firebase-id
   :visibility (if public? "Shared" "Private")
   :owner (<sub [:user-pretty-name owner])
   :text text
   :url url
   :tags (vec (sort tags))})


(defn modal-show-exports []
  (when (<sub [::fsm/in-page? :modal-show-exports])
    (let [lystros (<sub [::fsm/page-param])
          fn-abort  (na/>event [::fsm/goto :quit-modal])]
      [na/modal {:open? (<sub [::fsm/in-page? :modal-show-exports])
                 :dimmer "blurring"
                 :close-icon true
                 :close-on-dimmer-click? true
                 :on-close fn-abort}
       [na/modal-header {}
        (str "Exporting " (count lystros) " Lystros")
        [:div {:class "minor"}
         "Cut-n-paste to save"]]
       [na/modal-content {}
        [na/container {:class-name "literal-whitespace"}
         (with-out-str
           (cljs.pprint/pprint
            (mapv export-lystro lystros)))]]])))


