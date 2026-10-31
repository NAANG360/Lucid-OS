#include <sys/socket.h>
#include <sys/un.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <string>
#include <vector>
static constexpr const char* kSocket="/dev/socket/lucid-root";
static constexpr const char* kPolicy="/data/misc/lucid-root/allowed_uids";
static constexpr size_t kMax=16384;
static bool allowed(uid_t uid){std::ifstream f(kPolicy);unsigned long v;while(f>>v)if(v==(unsigned long)uid)return true;return false;}
static bool rd(int f,void* p,size_t n){char* c=(char*)p;while(n){ssize_t r=read(f,c,n);if(r<=0)return false;c+=r;n-=r;}return true;}
static bool wr(int f,const void* p,size_t n){const char* c=(const char*)p;while(n){ssize_t r=write(f,c,n);if(r<=0)return false;c+=r;n-=r;}return true;}
static void serve(int c){
 ucred cr{};socklen_t z=sizeof(cr);if(getsockopt(c,SOL_SOCKET,SO_PEERCRED,&cr,&z)<0||!allowed(cr.uid)){int e=126;wr(c,&e,sizeof(e));close(c);return;}
 uint32_t n=0;if(!rd(c,&n,sizeof(n))||!n||n>128){close(c);return;}
 std::vector<std::string>a;size_t total=0;
 for(uint32_t i=0;i<n;i++){uint32_t l=0;if(!rd(c,&l,4)||!l||l>4096||(total+=l)>kMax){close(c);return;}std::string s(l,'\\0');if(!rd(c,s.data(),l)||s.find('\\0')!=s.npos){close(c);return;}a.push_back(std::move(s));}
 if(a[0].empty()||a[0][0]!='/'){close(c);return;}
 int p[2];if(pipe2(p,O_CLOEXEC)<0){close(c);return;}pid_t pid=fork();
 if(pid==0){close(p[0]);int nullfd=open("/dev/null",O_RDONLY);if(nullfd>=0)dup2(nullfd,0);dup2(p[1],1);dup2(p[1],2);close(p[1]);std::vector<char*>v;for(auto&x:a)v.push_back(x.data());v.push_back(nullptr);execv(v[0],v.data());_exit(errno==ENOENT?127:126);}
 close(p[1]);char b[4096];ssize_t r;while((r=read(p[0],b,sizeof(b)))>0)if(!wr(c,b,(size_t)r))break;close(p[0]);
 int st=126;if(pid>0)waitpid(pid,&st,0);int rc=WIFEXITED(st)?WEXITSTATUS(st):(WIFSIGNALED(st)?128+WTERMSIG(st):126);wr(c,&rc,sizeof(rc));close(c);
}
int main(){int s=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);if(s<0)return 1;sockaddr_un a{};a.sun_family=AF_UNIX;strncpy(a.sun_path,kSocket,sizeof(a.sun_path)-1);unlink(kSocket);if(bind(s,(sockaddr*)&a,sizeof(a))<0||chmod(kSocket,0660)<0||listen(s,8)<0)return 2;for(;;){int c=accept4(s,nullptr,nullptr,SOCK_CLOEXEC);if(c<0){if(errno==EINTR)continue;continue;}serve(c);}}
