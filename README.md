Graph Algorithms Dataviz
===
A small collection of various algorithms from graph theory and network analysis I find interesting (implemented behind a compojure rest api) along with a potentially useful d3 visualization to show an actual use case (written in clojurescript, using reagent for components and secretary for routing).  This project is an excuse to learn the lay of the Clojure world a little better and dive into some interesting algorithms at the same time.


- *Closeness Centrality* - Modelling/visualization of the impact of fraud in a network of credit holders

- ..Some others...


[Closeness Centrality](https://en.wikipedia.org/wiki/Centrality#Closeness_centrality) can be used for determining the initial score of each node in the graph and a coefficient defined by the following function:

```
F(k) = (1 - (1/2)^k)
```

Where `k` is the length of the shortest path from a fraudulent customer to another affected customer in the network.


Instructions
---

To run the server:

```
lein run
```

*The basic client can be accessed from* http://localhost:3000

To run included tests:

```
lein test
```




API
---

A number of endpoints are exposed for interacting with the network:

**Edges index**
```
GET /edges
```
*Renders all available edges*


**Edge creation**
```
POST /edges
```
*POST a two element array (such as `[2, 3]`) to create a new edge with vertices :2 and :3*


**Vertices index**
```
GET /vertices?sort=asc|desc
```
*Gets all vertices ordered by score with optional `sort` query string param*



**Fraud attribute update**
```
PUT /vertices/:id
```
*PUT a json object of the shape `{fraudulent: true}`* to this endpoint to mark the vertex with id of `:id` as fraudulent



Client
---

A basic reagent + d3 force directed layout client that consumes this project's API is served from the root application directory

```
GET /
```

It provides an interactive visualization of the effects of the scoring algorithm used by the API (clicking a given node will mark it as fraudulent by calling the "Fraud attribute update" route documented above, and a subsequent fetch will allow the new scores to be rendered in the layout)
