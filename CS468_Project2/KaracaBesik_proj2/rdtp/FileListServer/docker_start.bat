docker build . -t file_server
@REM # docker run -d -t --rm -w "//app/tc" -p 5000:5000/udp -p 5001:5001/udp --name file_server --network=net file_server
docker run -d -t --rm -w "//app/tc" -p 5001:5001/udp -p 5002:5002/udp --name file_server -v files:/app/files -v C:/Users/emree/OneDrive/Masaüstü/CS468_Project2/KaracaBesik_proj2/rdtp/FileListServer/tc/policy1:/app/tc/policy1 -v C:/Users/emree/OneDrive/Masaüstü/CS468_Project2/KaracaBesik_proj2/rdtp/FileListServer/tc/policy2:/app/tc/policy2 file_server

@REM # following two lines runs server code in two ports 5000 and 5001
@REM # You can comment them and run server in the docker manually. In this way you, can see server logs
@REM # You can run server from cli of the docker simply by giving java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5000
docker exec -d -w "//app" -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5001
docker exec -d -w "//app" -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5002

@REM # for tc logs
docker logs --follow file_server