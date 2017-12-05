(ns vuagain.chromex.popup.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.loggers :refer [console]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]))

(defn display [display?]
  {:display (if display? "block" "none")})

(defn top-bar []
  [:nav {:class "navbar navbar-default"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:div {:class "navbar-brand"}]
     [:h3 "VuAgain"]]]])

(defn warning-bar []
  [:p {:class "warning",
       :id "warning",
       :style {:display "none"}}])


(defn bg-msg [message]
  (post-message! (<sub [:background-port])
                 (clj->js (assoc message :app "VuAgain"))))


(defn TOS-page [display?]
  [:form {:class "spaPage", :id "TOSForm", :style (display display?)}
   [:h4 "VuAgain Terms of Service"]
   [:p "VuAgain is in alpha pre-release, and may not yet be fully reliable."]
   [:p "Do not yet use VuAgain to store any information that you cannot afford to\nlose, or that you must keep confidential."]
   [:p "We plan to upgrade rapidly. Please contact our"
    [:a {:href "mailto:info@vuagain.com"}
     "support desk"]" with your comments\nand suggestions."]
   [:h5 "How to Use"]
   [:p "VuAgain helps you save notes about web pages you want to remember.\nUnlike bookmarking services, VuAgain does not force\nyou to look for your saved notes. Instead, they appear in your regular\nGoogle search results."]
   [:p "You can save two kinds of notes:"]
   [:ul
    [:li "Private notes are seen only by you"]
    [:li "Public comments can be seen by everyone"]]
   [:button {:class "btn btn-default", :id "acceptTOSButton", :type "button"} "I agree"]])

(defn logged-out-page [display?]
  [:div
   [:form {:class "spaPage", :id "anonymousForm", :style (display display?)}
    [:p "You do not seem to be logged in with a chrome identity."]
    [:p "VuAgain depends on features supplied by Chrome and cannot start until you are\nlogged in with a Chrome identity."]
    [:p "VuAgain will not work if you are using a non-Chrome browser or are logged in\nanonymously."]
    [:p "If you are seeing this message in other circumstances, please\ncontact our "
     [:a {:href "mailto:info@vuagain.com"} "support desk"]"."]]
   [:form {:class "spaPage", :id "loginForm", :style (display display?)}
    [:p "VuAgain needs to know your Google or Facebook identity to let you share\ncomments publicly or with your friends."]
    [:p {} (str "So ==>" (<sub [:user-name]) "<==")]
    [:button {:class "btn btn-social btn-google",
               :type "button"
               :id "socialLogin"
               :on-click #(bg-msg {:command "sign-in"})}
     "Login to VuAgain"]
    [TOS-page true]]])


(defn logged-in-page [display?]
  [:form {:class "spaPage", :id "vaForm", :style (display display?)}
   [:span
    [:span (<sub [:user-name])]
    [:button {:class "btn btn-social",
              :type "button"
              :id "logout"
              :on-click #(bg-msg {:command "sign-out"})}
     "Logout"]]        [:input {:class "form-control", :id "page", :type "hidden", :name "page"}]
   [:input {:id "originalPublicComment", :type "hidden", :name "originalPublicComment"}]
   [:input {:id "originalPrivateNote", :type "hidden", :name "originalPrivateNote"}]
   [:input {:id "rating", :type "hidden", :name "rating"}]
   [:input {:id "originalRating", :type "hidden", :name "originalRating"}]
   [:input {:id "visibility", :type "hidden", :name "visibility"}]
   [:input {:id "userFacebookId", :type "hidden", :name "userFacebookId"}]
   [:div {:class "form-group"}]
   [:div {:class "form-group"}
    [:label "Private Note"]
    [:input {:dir "auto",
             :placeholder "Note",
             :name "privateNote",
             :type "text",
             :maxLength "140",
             :id "privateNoteInput",
             :class "form-control",
             :autoFocus true,
             :autoComplete "off"}]]
   [:div {:class "form-group"}
    [:label "Public Comment"]
    [:input {:class "form-control",
             :id "publicCommentInput",
             :type "text",
             :placeholder "Comment",
             :name "publicComment",
             :autoComplete "off",
             :dir "auto",
             :maxLength "140"}]]
   [:label "Rating"]
   [:div {:class "form-group centered"}
    [:div {:class "btn-group btn-group-sm", :role "group"}
     [:button {:class "btn btn-default ratingButton", :id "hateButton", :type "button", :value "-1.0"}
      [:img {:src "images/hate-32.png"}]
      [:br]"hate"]
     [:button {:class "btn btn-default ratingButton", :id "dislikeButton", :type "button", :value "-0.5"}
      [:img {:src "images/dislike-32.png"}]
      [:br]"dislike"]
     [:button {:class "btn btn-default ratingButton", :id "soSoButton", :type "button", :value "0.0"}
      [:img {:src "images/so-so-32.png"}]
      [:br]"ok"]
     [:button {:class "btn btn-default ratingButton", :id "likeButton", :type "button", :value "0.5"}
      [:img {:src "images/like-32.png"}]
      [:br]"like"]
     [:button {:class "btn btn-default ratingButton", :id "loveButton", :type "button", :value "1.0"}
      [:img {:src "images/love-32.png"}]
      [:br]"love"]]]
   [:hr]
   [:div {:class "form-group centered"}
    [:button {:class "btn", :id "cancelButton"} "Cancel"]
    [:button {:class "btn btn-primary", :id "submitButton"} "Save"]]])

(defn footer-bar [display?]
  [:div {:class "panel-footer", :style {:margin-top "15px"}}
   [:small
    [:p {:class "alignLeft", :id "versionString"}]
    [:p {:class "alignRight"}
     [:a {:id "showTOSButton", :href "#"} "Show Terms of Service"]]]])

(defn popup []
  (fn []
    [:div
     [top-bar]
     [:div {:class "container"}
      [warning-bar]
      (if (<sub [:user])
        [logged-in-page true]
        [logged-out-page true])]
     [footer-bar true]]))
