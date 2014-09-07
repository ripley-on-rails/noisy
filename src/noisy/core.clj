(ns noisy.core
  (:import [java.awt.image BufferedImage]
           [java.awt Color]
           [java.util Random]
           [javax.imageio ImageIO]
           [java.io File]
           [com.google.common.hash Hashing])
  (:require [clojure.math.numeric-tower :as math]))

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
(defn murmur-random-generator
  "Returns a fn that given ints as input returns a deterministic PRN
   Double in [-1.0, 1.0]."
  ([]
     (murmur-random-generator 0))
  ([seed]
     (let [hashing (Hashing/murmur3_32 seed)]
       (fn [& vals]
(prn vals)
         (let [hasher (.newHasher hashing)]
           (doseq [val vals]
             (.putInt hasher val))
           (-> (.asInt (.hash hasher))
               normalize-int))))))

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
  (* t t t (+ 10
              (* -15 t)
              (* 6 t t))))

(defn linear-interpolation
  ([t]
     (linear-interpolation t 0 1))
  ([t a b]
     (+ a (* t (- b a)))))

(def p' [151,160,137,91,90,15,
   131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
   190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
   88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
   77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
   102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
   135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
   5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
   223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
   129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
   251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
   49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
   138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180])

(def p'' (vec (shuffle (range 256))))

(defn idp [x]
  (prn x)
  x)

;(def p (vec (concat p'' p'')))

(def p (murmur-random-generator))

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

; ported from http://mrl.nyu.edu/~perlin/noise/
(defn perlin2 [generator curve-fn]
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
_ (prn x')
          a (+ (p x') y')
          aa (+ (p a) z')
          ab (+ (p (inc a)) z')
          b (+ (p (inc x')) y')
          ba (+ (p b) z')
          bb (+ (p (inc b)) z')]
      (linear-interpolation
       w
       (linear-interpolation
        v
        (linear-interpolation u
                              (grad (p aa) x y z)
                              (grad (p ba) (dec x) y z))
        (linear-interpolation u
                              (grad (p ab) x (dec y) z)
                              (grad (p bb) (dec x) (dec y) z)))
       (linear-interpolation
        v
        (linear-interpolation u
                              (grad (p (inc aa)) x y (dec z))
                              (grad (p (inc ba)) (dec x) y (dec z)))
        (linear-interpolation u
                              (grad (p (inc ab)) x (dec y) (dec z))
                              (grad (p (inc bb)) (dec x) (dec y) (dec z))))))))

(defn perlin [generator curve-fn]
  (fn [& coords]
    (let [lower (map (comp int math/floor) coords)
          fractionals (map - coords lower)
          curves (map curve-fn fractionals)
          samples (sample-coordinates lower)
          ; the samples are ordered like so:
          ; (c/sample-coordinates [0 0 0])
          ; ([0 0 0] [1 0 0] [0 1 0] [1 1 0] [0 0 1] [1 0 1] [0 1 1] [1 1 1])
          ; therefore we can partion into 2 for each axis during
          ; the interpolation steps... pretty clever
          gradients (map (partial apply generator) samples)
          ]
      (loop [[curve & curves'] curves
             grads gradients]
        (let [groups (partition 2 grads)
              grads' (map (partial apply linear-interpolation curve)
                          groups)]
          (if (seq curves')
            (recur curves'
                   grads')
            (first grads')))))))

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
  (let [j (int (* 128 (inc i)))
;        j (mod (int i) 256)
        ]
    (.getRGB (Color. j j j))))

(defn paint1d [generator width height &
               {:keys [file grid] :or {file "image.png"}}]
  {:pre [width height generator]}
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        g2 (.createGraphics image)]
    (.setColor g2 Color/BLACK)
    (.fillRect g2 0 0 width height)
    (.setColor g2 (Color. 160 255 32))
    (doseq [x (range width)]
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
      (.setRGB image x y (gray (generator x y 0))))
    (when grid
      (.setColor g2 (Color. 255 128 0 128))
      (doseq [x (range 0 width grid)]
        (.drawLine g2 x 0 x height))
      (doseq [y (range 0 height grid)]
        (.drawLine g2 0 y width y)))
    (ImageIO/write image "png" (File. file))))


#_
(
 (require '[noisy.core :as c])
 (c/paint (-> (c/murmur-random-generator)
              (c/floor)
              (c/scale 10))
          200 200)

 (c/paint (-> (c/perlin (c/murmur-random-generator) c/fade1)
              (c/scale 10))
          200 200)
 )
