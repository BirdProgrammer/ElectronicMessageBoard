#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<unistd.h>
#include<arpa/inet.h>
#include<sys/socket.h>

void error_handling(char *message);

int main()
{
    int server_sock;
    struct sockaddr_in server_addr;
    int MAX_LENGTH=200;

    char message[MAX_LENGTH];
    int str_len = 0;

    server_sock = socket(PF_INET, SOCK_STREAM, 0);
    if(-1 == server_sock){
        error_handling("socket() error!");
        exit(1);
    }

    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = inet_addr("95.163.206.203");
    //server_addr.sin_addr.s_addr = inet_addr("127.0.0.1");
    server_addr.sin_port = htons(30000);

    if( -1 == connect(server_sock, (struct sockaddr*)&server_addr,
                      sizeof(server_addr)) ){
        error_handling("connect() error!");
    }

    printf("C:connect to server success...\n");
    
    int goon=1;
    //建立连接之后的处理逻辑
    memset(message, 32, sizeof(message));
    strcpy(message,"{\"label\":\"device\"}");
    write(server_sock, message, strlen(message));
    while(goon){
        strcpy(message,"{\"intent\":\"check\"}\0");
        write(server_sock, message, strlen(message));
        str_len = read(server_sock, message, MAX_LENGTH-1);
        message[str_len]='\0';
        printf("%s\n",message);
        fflush(stdout);
        usleep(1000000*2);
    }
    //断开连接，关闭套接字
    close(server_sock);

    return 0;
}

void error_handling(char *message)
{
    fputs(message, stderr);
    fputc('\n', stderr);
    exit(1);
}