;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-show-exports
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.fsm :as fsm]
   [trilystro.modal :as modal]))

(defn export-lystro [{:keys [firebase-id public? owner text url tags]}]
  {:uid firebase-id
   :visibility (if public? "Shared" "Private")
   :owner (<sub [:user-pretty-name owner])
   :text text
   :url url
   :tags (vec (sort tags))})


(defn view-modal-show-exports []
  (let [lystros (<sub [::fsm/page-param])]
    [modal/modal {:page :modal-show-exports
                  :header (str "Exporting " (count lystros) " Lystros")}
     [na/container {:class-name "literal-whitespace"}
      (with-out-str
        (cljs.pprint/pprint
         (mapv export-lystro lystros)))]]))


