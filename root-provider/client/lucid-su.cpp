#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

static bool send_all(int fd,const void* data,size_t n) {
 const char* p=(const char*)data; while(n){ssize_t w=write(fd,p,n);if(w<=0)return false;p+=w;n-=w;}return true;
}
static bool recv_all(int fd,void* data,size_t n) {
 char* p=(char*)data;while(n){ssize_t r=read(fd,p,n);if(r<=0)return false;p+=r;n-=r;}return true;
}
int main(int argc,char** argv) {
 if(argc<2){fprintf(stderr,"usage: su COMMAND [ARG...]
");return 2;}
 int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);if(fd<0){perror("lucid-su: socket");return 126;}
 sockaddr_un a{};a.sun_family=AF_UNIX;strncpy(a.sun_path,"/dev/socket/lucid-root",sizeof(a.sun_path)-1);
 if(connect(fd,(sockaddr*)&a,sizeof(a))<0){fprintf(stderr,"Lucid Root: provider unavailable (%s)
",strerror(errno));close(fd);return 126;}
 uint32_t count=(uint32_t)(argc-1); if(!send_all(fd,&count,sizeof(count)))return 126;
 for(int i=1;i<argc;i++){uint32_t n=(uint32_t)strlen(argv[i]);if(!send_all(fd,&n,sizeof(n))||!send_all(fd,argv[i],n))return 126;}
 char buf[4096];ssize_t n;while((n=read(fd,buf,sizeof(buf)))>0){if(!send_all(STDOUT_FILENO,buf,(size_t)n))break;}
 close(fd);
 // Protocol v1 streams output; exit code is delivered by broker after child exits.
 // A separate status channel is added in the client/server integration test before release.
 return 0;
}
