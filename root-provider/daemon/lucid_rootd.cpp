#include <sys/socket.h>
#include <sys/un.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>

static constexpr const char* kSocket = "/dev/socket/lucid-root";
static constexpr const char* kPolicy = "/data/misc/lucid-root/allowed_uids";
static constexpr size_t kMax = 16384;

static bool allowed(uid_t uid) {
    std::ifstream in(kPolicy);
    unsigned long value;
    while (in >> value) if (value == static_cast<unsigned long>(uid)) return true;
    return false;
}
static bool read_all(int fd, void* data, size_t size) {
    char* p = static_cast<char*>(data);
    while (size) { ssize_t n=read(fd,p,size); if(n<=0) return false; p+=n; size-=n; }
    return true;
}
static bool write_all(int fd, const void* data, size_t size) {
    const char* p = static_cast<const char*>(data);
    while (size) { ssize_t n=write(fd,p,size); if(n<=0) return false; p+=n; size-=n; }
    return true;
}
static void serve(int client) {
    ucred cred{}; socklen_t clen=sizeof(cred);
    if (getsockopt(client,SOL_SOCKET,SO_PEERCRED,&cred,&clen)<0 || !allowed(cred.uid)) {
        const int denied=126; write_all(client,&denied,sizeof(denied)); close(client); return;
    }
    uint32_t count=0;
    if(!read_all(client,&count,sizeof(count)) || count==0 || count>128) { close(client); return; }
    std::vector<std::string> args; args.reserve(count);
    size_t total=0;
    for(uint32_t i=0;i<count;i++) {
        uint32_t len=0;
        if(!read_all(client,&len,sizeof(len)) || len==0 || len>4096 || (total+=len)>kMax) { close(client); return; }
        std::string s(len,'\0');
        if(!read_all(client,s.data(),len) || s.find('\0')!=std::string::npos) { close(client); return; }
        args.push_back(std::move(s));
    }
    if(args.empty() || args[0].empty() || args[0][0]!='/') { close(client); return; }
    pid_t pid=fork();
    if(pid==0) {
        dup2(client,STDIN_FILENO); dup2(client,STDOUT_FILENO); dup2(client,STDERR_FILENO);
        close(client);
        std::vector<char*> argv; for(auto& a:args) argv.push_back(a.data()); argv.push_back(nullptr);
        execv(argv[0],argv.data()); _exit(errno==ENOENT?127:126);
    }
    int status=126;
    if(pid>0) waitpid(pid,&status,0);
    int rc=WIFEXITED(status)?WEXITSTATUS(status):(WIFSIGNALED(status)?128+WTERMSIG(status):126);
    write_all(client,&rc,sizeof(rc)); close(client);
}
int main() {
    int s=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0); if(s<0) return 1;
    sockaddr_un addr{}; addr.sun_family=AF_UNIX; strlcpy(addr.sun_path,kSocket,sizeof(addr.sun_path));
    unlink(kSocket);
    if(bind(s,reinterpret_cast<sockaddr*>(&addr),sizeof(addr))<0 || chmod(kSocket,0660)<0 || listen(s,8)<0) return 2;
    for(;;) { int c=accept4(s,nullptr,nullptr,SOCK_CLOEXEC); if(c<0) { if(errno==EINTR) continue; continue; } serve(c); }
}
