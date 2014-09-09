(ns noisy.core
  (:import [java.awt.image BufferedImage]
           [java.awt Color]
           [java.util Random]
           [javax.imageio ImageIO]
           [java.io File]
           [com.google.common.hash Hashing])
  (:require [clojure.math.numeric-tower :as math]
            [clojure.core.cache :as cache]))

(defn- normalize-int [i]
  (cond
   (pos? i) (double (/ i Integer/MAX_VALUE))
   (neg? i) (- (double (/ i Integer/MIN_VALUE)))
   :else i))

; TODO: here's an idea: use caching/memoization as a 'middleware'/proxy
;       to the rnd-gen. We only need to cache 'width' many entries when
;       traversing x/y... 'height' in the y/x case.
;       -> follow-up idea traverse to minimize storage requirement

;;; implicit functions
(defn murmur-prng
  ([] (murmur-prng 0))
  ([seed]
     (let [hashing (Hashing/murmur3_32 seed)]
       (fn [& vals]
         (let [hasher (.newHasher hashing)]
           (doseq [val vals]
             (.putInt hasher val))
           (.asInt (.hash hasher)))))))

(defn murmur-noise [& {:keys [seed]
                       :or {seed 0}}]
  (comp normalize-int (murmur-prng seed)))

(def gradient-vectors [[1 1 0] [-1 1 0] [1 -1 0] [-1 -1 0]
                       [1 0 1] [-1 0 1] [1 0 -1] [-1 0 -1]
                       [0 1 1] [0 -1 1] [0 1 -1] [0 -1 -1]])

(defn murmur-gradient-generator [& {:keys [gradients seed]
                                    :or {gradients gradient-vectors
                                         seed 0}}]
  (let [gradient-count (count gradients)
        prng (murmur-prng seed)]
    (fn [& coords]
       (gradients (mod (apply prng coords)
                      gradient-count)))))

(defn permutation-table [& {:keys [seed table-size]
                            :or {seed 0
                                 table-size 256}}]
  (let [rnd (java.util.Random. seed)]
    (loop [src (vec (range table-size))
           dst []]
      (if (seq src)
        (let [i (.nextInt rnd (count src))]
          (recur (vec (concat (take i src)
                              (drop (inc i) src)))
                 (conj dst (nth src i))))
        (vec (concat dst dst))))))

(defn rotate [n s]
  (let [[front back] (split-at (mod n (count s)) s)]
    (vec (concat back front))))

(def p
  (let [p' [151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,
            103,30,69,142,8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,
            26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,
            87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,
            77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,
            46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,
            187,208,89,18,169,200,196,135,130,116,188,159,86,164,100,109,
            198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,
            255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,
            183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,
            43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,
            112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,179,
            162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,
            106,157,184,84,204,176,115,121,50,45,127,4,150,254,138,236,
            205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,
            180]]
    (vec (concat p' p'))))

(defn grad [hash x y z]
  (let [h (bit-and hash 15)
        u (if (< h 8) x y)
        v (if (< h 4)
            y
            (if (or (= h 12)
                    (= h 14))
              x
              z))]
    (+ (if (zero? (bit-and h 1))
         u
         (- u))
       (if (zero? (bit-and h 2))
         v
         (- v)))))

;;; generators
(defn sample-coordinates [lower]
  (reduce (fn [acc v]
            (let [upper (inc v)]
              (if (seq acc)
                (for [new [v upper]
                      old acc]
                  (conj old new))
                [[v] [upper]])))
          nil
          lower))

(defn fade1 [t]
  (* t t (- 3 (* 2 t))))

(defn fade2 [t]
  (* t t t (+ 10 (* t (+ (* 6 t) -15 )))))

(defn linear-interpolation
  ([t]
     (linear-interpolation t 0 1))
  ([t a b]
     (+ a (* t (- b a)))))

(defn normalize [v src-min src-max dst-min dst-max]
  (+ (/ (* (- v src-min)
           (- dst-max dst-min))
        (- src-max src-min))
     dst-min))

(defn make-cache-fn [source threshold]
  (let [c (atom (cache/fifo-cache-factory {} :threshold threshold))]
    (fn [& coords]
      (if (cache/has? @c coords)
        (cache/lookup @c coords)
        (let [val (apply source coords)]
          (do
            (swap! c assoc coords val)
            val))))))

; ported from http://mrl.nyu.edu/~perlin/noise/
(defn perlin-improved [& {:keys [curve-fn seed] :or {curve-fn fade2}}]
  (let [table (if seed
                (permutation-table :seed seed)
                p)]
    (fn [x y z]
      (let [x' (bit-and (int (math/floor x)) 255)
            y' (bit-and (int (math/floor y)) 255)
            z' (bit-and (int (math/floor z)) 255)
            x (- x (math/floor x))
            y (- y (math/floor y))
            z (- z (math/floor z))
            u (curve-fn x)
            v (curve-fn y)
            w (curve-fn z)
            a (+ (table x') y')
            aa (+ (table a) z')
            ab (+ (table (inc a)) z')
            b (+ (table (inc x')) y')
            ba (+ (table b) z')
            bb (+ (table (inc b)) z')]
        (linear-interpolation
         w
         (linear-interpolation
          v
          (linear-interpolation u
                                (grad (table aa) x y z)
                                (grad (table ba) (dec x) y z)
                                )
          (linear-interpolation u
                                (grad (table ab) x (dec y) z)
                                (grad (table bb) (dec x) (dec y) z)))
         (linear-interpolation
          v
          (linear-interpolation u
                                (grad (table (inc aa)) x y (dec z))
                                (grad (table (inc ba)) (dec x) y (dec z)))
          (linear-interpolation u
                                (grad (table (inc ab)) x (dec y) (dec z))
                                (grad (table (inc bb)) (dec x) (dec y) (dec z)))))))))

(defn dot-prod [a b]
  (apply + (map * a b)))

(defn perlin [& opts]
  (let [{:keys [curve-fn seed cache]
         :or {seed 0
              curve-fn fade2}} opts
        grad-gen' (murmur-gradient-generator :seed seed)
        {:keys [grad-gen]
         :or {grad-gen (if cache
                         (make-cache-fn grad-gen' cache)
                         grad-gen')}} opts]
    (fn [& coords]
      (let [lower (map (comp int math/floor) coords)
            fractionals (map - coords lower)
            curves (map curve-fn fractionals)
            samples (sample-coordinates lower)
            ;; the samples are ordered like so:
            ;; (c/sample-coordinates [0 0 0])
            ;; ([0 0 0] [1 0 0] [0 1 0] [1 1 0] [0 0 1] [1 0 1] [0 1 1] [1 1 1])
            ;; therefore we can partion into 2 for each axis during
            ;; the interpolation steps... pretty clever
            gradients (map (fn [sample]
                             (let [fractionals' (map +
                                                     fractionals
                                                     (map - lower sample))]
                               (/
                                (dot-prod
                                 fractionals'
                                 (apply grad-gen sample))
                                1)))
                           samples)]
        (loop [[curve & curves'] curves
               grads gradients]
          (let [groups (partition 2 grads)
                grads' (map (partial apply linear-interpolation curve)
                            groups)]
            (if (seq curves')
              (recur curves'
                     grads')
              (first grads'))))))))

;;; modifiers
(defn scale [source & factors]
  (fn [& coords]
    {:pre [(or (= 1 (count factors))
               (= (count factors)
                  (count coords)))]}
    (apply source
           (map #(/ % %2)
                coords
                (if (= 1 (count factors))
                  (repeat (first factors))
                  factors)))))

(defn floor [source]
  (fn [& coords]
    (apply source (map #(int (math/floor %)) coords))))

;;; RGBA functions
(defn- gray
  "Returns a gray-scale rgb representation given [-1.0, 1.0] as input"
  [i]
  (let [j (int (normalize i -1 1 0 255))
;        j (mod (int i) 256)
        ]
    (.getRGB (Color. j j j))))

(defn color->rgba [c]
  [(.getRed c) (.getGreen c) (.getBlue c) (.getAlpha c)])

;; Modifiers
(defn modify [source f]
  (fn [& coords]
    (f (apply source coords))))

(defn abs [source]
  (modify source math/abs))

(defn invert [source]
  (modify source -))

(defn normalize-modifier
  ([source min1 max1]
     (normalize-modifier source min1 max1 -1 1))
  ([source min1 max1 min2 max2]
     (modify source
             #(normalize % min1 max1 min2 max2))))

(defn rgba-interpolation [v c1 c2]
  (apply #(Color. % %2 %3 %4)
         (map (comp int (partial linear-interpolation v))
              (color->rgba c1)
              (color->rgba c2))))

(defn gradient [& opts]
  (let [[c1 t1] (take 2 opts)
        transitions (partition 3 (drop 2 opts))]
    (fn [val]
      (.getRGB
       (if (> t1 val)
         c1
         (loop [c1 c1
                t1 t1
                [[interp c2 t2] & transitions'] transitions]
           (if interp
             (if (> val t2)
               (recur c2 t2 transitions')
               (interp (/ (- val t1) (- t2 t1)) c1 c2))
             c1)))))))



(def mini (atom nil))

(def maxi (atom nil))

(defn paint1d [generator width height &
               {:keys [file grid] :or {file "image.png"}}]
  {:pre [width height generator]}
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        g2 (.createGraphics image)]
    (.setColor g2 Color/BLACK)
    (.fillRect g2 0 0 width height)
    (.setColor g2 (Color. 160 255 32))
    (doseq [x (range width)]
      (let [val (generator x 0 0)]
        (reset! mini (if @mini (min @mini val) val))
        (reset! maxi (if @maxi (max @maxi val) val)))
      (let [value (* height (/ (inc (generator x 0 0)) 2))]
        (.drawLine g2 x value x height)))
    (when grid
      (.setColor g2 (Color. 255 128 0 128))
      (doseq [x (range 0 width grid)]
        (.drawLine g2 x 0 x height))
      (.drawLine g2 0 (/ height 2) width (/ height 2)))
    (ImageIO/write image "png" (File. file))))

(defn paint2d [generator width height &
               {:keys [file grid] :or {file "image.png"}}]
  {:pre [width height generator]}
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        g2 (.createGraphics image)]
    (doseq [x (range width)
            y (range height)]
      #_(let [val (generator x y 0)]
        (reset! mini (if @mini (min @mini val) val))
        (reset! maxi (if @maxi (max @maxi val) val)))
      (let [val (generator x y 0)
            color (if (instance? Color val)
                    val
                    (gray val))]
        (.setRGB image x y color)))
    (when grid
      (.setColor g2 (Color. 255 128 0 128))
      (doseq [x (range 0 width grid)]
        (.drawLine g2 x 0 x height))
      (doseq [y (range 0 height grid)]
        (.drawLine g2 0 y width y)))
    (ImageIO/write image "png" (File. file))))
