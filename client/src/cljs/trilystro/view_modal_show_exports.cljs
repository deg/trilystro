;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-show-exports
  (:require
   [cljs.pprint :as pprint]
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [iron.re-utils :refer [<sub]]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]
   [trilib.modal :as modal]))

(defn export-lystro [{:keys [firebase-id shared? owner text url tags]}]
  {:uid firebase-id
   :visibility (if shared? "Shared" "Private")
   :owner (<sub [::fb/user-pretty-name owner])
   :text text
   :url url
   :tags (vec (sort tags))})


(defn view-modal-show-exports []
  (let [lystros (<sub [::fsm/page-param])]
    [modal/modal {:page :modal-show-exports
                  :header (str "Exporting " (count lystros) " Lystros")}
     [na/container {:class-name "literal-whitespace"}
      (with-out-str
        (pprint/pprint (mapv export-lystro lystros)))]]))


