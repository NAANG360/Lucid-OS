#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
static bool wr(int f,const void*p,size_t n){const char*c=(const char*)p;while(n){ssize_t x=write(f,c,n);if(x<=0)return false;c+=x;n-=x;}return true;}
int main(int ac,char**av){if(ac<2){fprintf(stderr,"usage: su COMMAND [ARG...]\\n");return 2;}int f=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);if(f<0)return 126;sockaddr_un a{};a.sun_family=AF_UNIX;strncpy(a.sun_path,"/dev/socket/lucid-root",sizeof(a.sun_path)-1);if(connect(f,(sockaddr*)&a,sizeof(a))<0){fprintf(stderr,"Lucid Root unavailable: %s\\n",strerror(errno));return 126;}uint32_t n=ac-1;if(!wr(f,&n,4))return 126;for(int i=1;i<ac;i++){uint32_t l=strlen(av[i]);if(!wr(f,&l,4)||!wr(f,av[i],l))return 126;}shutdown(f,SHUT_WR);std::vector<char> tail;char b[4096];ssize_t r;while((r=read(f,b,sizeof(b)))>0){tail.insert(tail.end(),b,b+r);if(tail.size()>4){size_t out=tail.size()-4;wr(1,tail.data(),out);tail.erase(tail.begin(),tail.begin()+out);}}close(f);if(tail.size()!=4)return 126;int rc;memcpy(&rc,tail.data(),4);return rc;}
