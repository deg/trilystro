;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.view-modal-about
  (:require
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.db :as db]
   [trilystro.fsm :as fsm]
   [trilystro.fsm-graph :as fsm-graph]
   [trilystro.modal :as modal]))


(defn view-modal-about []
  [modal/modal {:page :modal-about
                :header (str "About " (<sub [::db/name]))}
   [na/container {}
    [:div {:class "about"}
     "Trilystro is still a toy app, playing with ideas about data curation, Firebase,
     re-frame, and reactive websites. But, it is slowly becoming useful to me as a real
     tool, so here are some quick docs:"]
    [:div {:class "about"}
     "A Lystro is a reference to an interesting fact. It includes multiple tags, a url,
     and free text. It can be private to you, or shared with the public."]
    [:div {:class "about"}
     "You can login or logout by clicking on the field at the right of the menu bar."]
    [:div {:class "about"}
     "You can search for Lystros by any of these fields. Most are obvious, but there
     are two subtleties:"]
    [:ul
     [:li {:class "about"}
      "You can search for Lystros that match any of the tags you select, or you can
       restrict to match only lystros that contain all of your selected tags."]
     [:li {:class "about"}
      "Text search is case-insensitive. By default, it matches against only the
      text of each Lystro, or you can click the icons at the top corners of the
      search field to also match for that text in the URL or tags fields."]]
    [:div {:class "about"}
     "You can create a new Lystro by clicking on 'Add' in the menu bar and filling
      in the fields."]
    [:div {:class "about"}
     "You can edit or delete a Lystro that belongs to you by clicking on the buttons
      at its top right. You can also edit by clicking on the Lystro's text. Clicking
      on the tags adds or removes them from the current search. Clicking on the URL
      follows the link."]
    [:div {:class "about"}
     "The export all and export current buttons at the bottom of the page generate
      an EDN representation of the Lystros. You can cut-n-paste to save externally."]
    [:div {:class "about"}
     "(Mostly for my own amusement/debugging), the following is the FSM navigated
     by the app."]
    (fsm-graph/render-graph (<sub [::fsm/page-graph]))
    [:div {:class "credits"} "Copyright (c) 2017, David Goldfarb (deg@degel.com)"]
    [:div {:class "credits"}
     (let [{:keys [commit date]} (<sub [:git-commit])]
       (str "This version built from GIT commit: " commit " of " date))]]])
