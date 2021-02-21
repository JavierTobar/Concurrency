# Fault Line terrain generator

### Summary

There's a NxN terrain. 

You want to pseudo randomize the terrain layout by modifying the height of the given terrain.

The fault line algorithm is as follows :

- A line crosses your terrain from edge A to edge B such that A ≠ B.
- It pseudo randomly picks one side of the line, and elevates all the points on that side by a given amount.
- Repeat

However, we want to do this with concurrency to improve performance. 

Thus, we have multiple threads modifying the same terrain and they cannot collide with each other but they must be able to run concurrently.
