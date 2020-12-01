# Final Project Write Up
## EECE 4371 - Philip Ferreira

### What this project does
This project, while running on a client, an intermediate server, and a storage server, allows the user to upload and download files to and from an external harddrive connected to a raspberry pi. The intermediate server allows the client to discover the storage device anywhere without knowing its exact IP address.

### Why I chose it
I chose this project becasue I wanted a networking challenge and I really wanted to use a rapsberry pi because I think they are cool. I also wanted to create something useful and thought that backing up files to a personal harddrive was an interesting concept

### Technical Problems I faced
My first technical problem was figuring out how to connect the client to storage via the intermediate server. The storage could directly access the server, and the client could also, but the reverse isn't necessarily true. I wanted a direct path from client to storage and back, and that was a puzzle to solve

My next technical problem was figuring out how to efficiently transmit files via the internet. How would I handle directories with other files and directories? How do I use the intermediate server to connect the client and storage? How do I minimize the disk usage on the intermediate server since it's a basic EC2 instance? This issue perhaps took the longest to figure out

### How I solved these issues
I solved the first issue by treating the storage/intermediate server pair as a whole. The client only "sees" a single server to communicate to. Next, I had the storage and intermediate establish and maintain a constant TCP connection so the server could always "push" new requests to the storage. The client would simple send requests and data, and get a response back.

The second issue, as mentioned earllier, took the longest time to solve. First, I decided that every file sent (file or directory) would be first compressed into a zip file to both minimize size and reduce transfers to a single file. Next, the intermediate servers keeps two sockets (one for client, one for storage) and essentially pipes the input of one to the output of the other for the file transfers. This also minimizes disk usage because the intermediate server isn't writing any new files. As soon as it gets some data from one socket, it sends it off to the other. 

Figuring out the exact solution took a while. The final result was to send the file size as part of the request so each link knew how much data to write. When the bytes written equaled the file size, it was done. This also accounts for differences in network speed because it counts data written not times read.

