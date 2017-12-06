(ns trilystro.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [trilystro.core-test]))

(doo-tests 'trilystro.core-test)
