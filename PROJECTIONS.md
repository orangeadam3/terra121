# Map Projections

It is mathmatically imposible to create a perfect map of a globe and minecraft is inherently flat. Therefore, some distortion must be made when creating the world. The way this distortion is expressed can vary based on what properties the user wants. This is generaly a trade-off between shape accuracy and area accuracy. Any projection can be described as two equations relating longitude and latitude to the 2D x/y coordinates.

```
λ = longitude
φ = latitude
x = x-coordinate
y = y-coordinate
```

## Equirectangular
```
x = λ
y = φ
```

![Source: Wikipedia](https://upload.wikimedia.org/wikipedia/commons/8/83/Equirectangular_projection_SW.jpg)

This is just a simple projection that does no transformation on the latitude and longitude. This is the fastest and simplest projection. It is the default projection, and has fairly decent shape and area preservation as long as you are not near the poles.

[wikipedia](https://en.wikipedia.org/wiki/Equirectangular_projection)

## Sinusoidal
```
x = λcosφ
y = φ
```

![Source: Wikipedia](https://upload.wikimedia.org/wikipedia/commons/b/b9/Sinusoidal_projection_SW.jpg)

This is an equal area projection so any region will have the exact same area in the game as they do on earth. This comes at the cost of high shape distortion This projection is fairly fast will give you the best performance of any equal area projection. (except mabye Gall-Peters (not supported currently))

[wikipedia](https://en.wikipedia.org/wiki/Sinusoidal_projection)

## Mercator
```
x = λ in radians
y = log(tan(½φ + 45°)))
```

![Source: Wikipedia](https://upload.wikimedia.org/wikipedia/commons/7/73/Mercator_projection_Square.JPG)

This is the most common projection and is similar the ones used on openstreetmap, google maps, and most modern maps. The shapes (technically the angles) are perfect but the areas are famously way off near the pole (Greenland is not the same size as Africa, dispite what this map says). It is also fairly slow so may lag more than simpler projections.

[wikipedia](https://en.wikipedia.org/wiki/Mercator_projection)

## Equal Earth
![Source: Wikipedia](https://wikimedia.org/api/rest_v1/media/math/render/svg/9d39c578b7c78436d1b7a33608ab7436ecc5e9dd)

where

![Source: Wikipedia](https://wikimedia.org/api/rest_v1/media/math/render/svg/e1be68e5b603219a58709c9a42e6995d060969b7)

![Source: Wikipedia](https://upload.wikimedia.org/wikipedia/commons/6/61/Equal_Earth_projection_SW.jpg)

This is a relatively new projection. It maintains area but also tries to have less shape distortion than Sinusoidal. It was invented by Bojan Šavrič, Bernhard Jenny, and Tom Patterson in 2018. The formula is by far the most complicated of those on this list and it is the slowest. (Reversing it requires solving a 9th degree polynomial)

[wikipedia](https://en.wikipedia.org/wiki/Equal_Earth_projection)
