#include <windows.h>
#include <shellapi.h>
#include <shobjidl.h>
#include <winhttp.h>
#include <wrl.h>
#include <filesystem>
#include <string>
#include <vector>
#include "WebView2.h"
#include "WebView2EnvironmentOptions.h"

#pragma comment(lib, "winhttp.lib")

using Microsoft::WRL::Callback;
using Microsoft::WRL::ComPtr;

namespace {
ComPtr<ICoreWebView2Controller> controller;
ComPtr<ICoreWebView2> webview;
HWND windowHandle = nullptr;
HINSTANCE appInstance = nullptr;

ComPtr<ICoreWebView2Environment> sharedEnvironment;
std::wstring startUrl = L"http://127.0.0.1:8848/";
std::wstring userDataDirectory;
bool initialisationFailed = false;
bool smokeTest = false;
bool navigationSucceeded = false;
bool showingLoadingPage = true;
unsigned int probeAttemptCount = 0;
NOTIFYICONDATAW trayIcon{};
bool trayIconAdded = false;
UINT taskbarCreatedMessage = 0;
HICON applicationIcon = nullptr;

constexpr UINT WM_TRAY_ICON = WM_APP + 1;
constexpr UINT ID_STARTUP_RETRY_TIMER = 2;
constexpr UINT ID_PROBE_TIMER = 3;
constexpr unsigned int MAX_STARTUP_RETRIES = 120;
constexpr UINT ID_TRAY_OPEN = 1001;
constexpr UINT ID_TRAY_EXIT = 1002;
constexpr UINT ID_TRAY_VERSION = 1003;
constexpr UINT ID_TRAY_CHECK_UPDATE = 1004;
constexpr UINT ID_TRAY_LAN = 1005;

bool startsWith(const std::wstring& value, const wchar_t* prefix) {
    return value.rfind(prefix, 0) == 0;
}

bool isAllowedUrl(const std::wstring& url) {
    return url == L"about:blank" || startsWith(url, L"http://127.0.0.1:") || startsWith(url, L"http://localhost:");
}

const wchar_t* loadingPageHtml() {
    return LR"HTML(<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><style>
:root{color-scheme:dark}
*{box-sizing:border-box}
html,body{height:100%;margin:0}
body{display:flex;align-items:center;justify-content:center;background:radial-gradient(circle at 50% 35%,#064e3b 0%,#022c22 80%);font-family:-apple-system,"Segoe UI","Microsoft YaHei",sans-serif;color:#ecfdf5}
.splash{text-align:center}
.mark{width:72px;height:72px;margin:0 auto 24px;border-radius:20px;background:linear-gradient(135deg,#10b981,#047857);display:flex;align-items:center;justify-content:center;font-size:32px;font-weight:700;color:#ffffff;box-shadow:0 0 45px rgba(16,185,129,.45);animation:pulse 2.2s ease-in-out infinite}
h1{font-size:20px;font-weight:600;margin:0 0 8px;letter-spacing:.02em}
p{margin:0;font-size:13px;color:#a7f3d0}
.dots span{display:inline-block;width:6px;height:6px;margin:0 3px;border-radius:50%;background:#10b981;opacity:.3;animation:blink 1.4s infinite}
.dots span:nth-child(2){animation-delay:.2s}
.dots span:nth-child(3){animation-delay:.4s}
@keyframes blink{0%,80%,100%{opacity:.25}40%{opacity:1}}
@keyframes pulse{0%,100%{transform:scale(1)}50%{transform:scale(1.06)}}
@media(prefers-reduced-motion:reduce){.mark,.dots span{animation:none}}
</style></head><body><div class="splash"><div class="mark">📦</div><h1>Collecter 资产与收纳管家 正在启动</h1><p>正在装载本地私有沙盒与 12 馆时效引擎<span class="dots"><span></span><span></span><span></span></span></p></div></body></html>)HTML";
}

bool probeBackendReady() {
    URL_COMPONENTS components{};
    components.dwStructSize = sizeof(components);
    wchar_t hostBuffer[256]{};
    components.lpszHostName = hostBuffer;
    components.dwHostNameLength = ARRAYSIZE(hostBuffer);
    components.dwSchemeLength = static_cast<DWORD>(-1);
    components.dwUrlPathLength = static_cast<DWORD>(-1);
    if (!WinHttpCrackUrl(startUrl.c_str(), 0, 0, &components)) return false;

    bool ready = false;
    HINTERNET session = WinHttpOpen(L"CollecterWindow/1.0", WINHTTP_ACCESS_TYPE_NO_PROXY,
        WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (session) {
        HINTERNET connect = WinHttpConnect(session, hostBuffer, components.nPort, 0);
        if (connect) {
            HINTERNET request = WinHttpOpenRequest(connect, L"GET", L"/api/v1/health",
                nullptr, WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES, 0);
            if (request) {
                WinHttpSetTimeouts(request, 800, 800, 800, 800);
                if (WinHttpSendRequest(request, WINHTTP_NO_ADDITIONAL_HEADERS, 0, WINHTTP_NO_REQUEST_DATA, 0, 0, 0)
                        && WinHttpReceiveResponse(request, nullptr)) {
                    DWORD statusCode = 0, statusCodeSize = sizeof(statusCode);
                    WinHttpQueryHeaders(request, WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                        WINHTTP_HEADER_NAME_BY_INDEX, &statusCode, &statusCodeSize, WINHTTP_NO_HEADER_INDEX);
                    ready = (statusCode >= 200 && statusCode < 500);
                }
                WinHttpCloseHandle(request);
            }
            WinHttpCloseHandle(connect);
        }
        WinHttpCloseHandle(session);
    }
    return ready;
}

void registerTrayIcon(HWND hwnd) {
    if (trayIconAdded) return;
    trayIcon.cbSize = sizeof(NOTIFYICONDATAW);
    trayIcon.hWnd = hwnd;
    trayIcon.uID = 1;
    trayIcon.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
    trayIcon.uCallbackMessage = WM_TRAY_ICON;
    trayIcon.hIcon = applicationIcon ? applicationIcon : LoadIcon(nullptr, IDI_APPLICATION);
    wcscpy_s(trayIcon.szTip, L"Collecter 资产与收纳管家");
    trayIconAdded = Shell_NotifyIconW(NIM_ADD, &trayIcon) == TRUE;
}

void removeTrayIcon() {
    if (!trayIconAdded) return;
    Shell_NotifyIconW(NIM_DELETE, &trayIcon);
    trayIconAdded = false;
}

void showTrayMenu(HWND hwnd) {
    POINT pt;
    GetCursorPos(&pt);
    HMENU menu = CreatePopupMenu();
    InsertMenuW(menu, 0, MF_BYPOSITION | MF_STRING, ID_TRAY_OPEN, L"✨ 打开资产管家");
    InsertMenuW(menu, 1, MF_BYPOSITION | MF_STRING, ID_TRAY_LAN, L"⚡ 局域网大屏互传");
    InsertMenuW(menu, 2, MF_BYPOSITION | MF_STRING, ID_TRAY_CHECK_UPDATE, L"🔄 检查版本更新");
    InsertMenuW(menu, 3, MF_BYPOSITION | MF_SEPARATOR, 0, nullptr);
    InsertMenuW(menu, 4, MF_BYPOSITION | MF_STRING, ID_TRAY_EXIT, L"❌ 退出应用");

    SetForegroundWindow(hwnd);
    TrackPopupMenu(menu, TPM_RIGHTBUTTON, pt.x, pt.y, 0, hwnd, nullptr);
    DestroyMenu(menu);
}

void updateBounds() {
    if (!controller || !windowHandle) return;
    RECT bounds;
    GetClientRect(windowHandle, &bounds);
    controller->put_Bounds(bounds);
}

void navigateToStartUrl() {
    if (!webview) return;
    showingLoadingPage = false;
    webview->Navigate(startUrl.c_str());
}

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    if (taskbarCreatedMessage && msg == taskbarCreatedMessage) {
        trayIconAdded = false;
        registerTrayIcon(hwnd);
        return 0;
    }

    switch (msg) {
    case WM_SIZE:
        updateBounds();
        return 0;
    case WM_TIMER:
        if (wParam == ID_PROBE_TIMER) {
            probeAttemptCount++;
            if (probeBackendReady()) {
                KillTimer(hwnd, ID_PROBE_TIMER);
                navigateToStartUrl();
            } else if (probeAttemptCount >= MAX_STARTUP_RETRIES) {
                KillTimer(hwnd, ID_PROBE_TIMER);
                MessageBoxW(hwnd, L"后端服务启动超时，请检查服务运行日志。", L"Collecter 启动失败", MB_OK | MB_ICONERROR);
            }
            return 0;
        }
        break;
    case WM_TRAY_ICON:
        if (lParam == WM_LBUTTONUP || lParam == WM_LBUTTONDBLCLK) {
            ShowWindow(hwnd, SW_RESTORE);
            SetForegroundWindow(hwnd);
        } else if (lParam == WM_RBUTTONUP) {
            showTrayMenu(hwnd);
        }
        return 0;
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case ID_TRAY_OPEN:
            ShowWindow(hwnd, SW_RESTORE);
            SetForegroundWindow(hwnd);
            break;
        case ID_TRAY_LAN:
            ShellExecuteW(nullptr, L"open", L"http://127.0.0.1:8848", nullptr, nullptr, SW_SHOWNORMAL);
            break;
        case ID_TRAY_CHECK_UPDATE:
            MessageBoxW(hwnd, L"Collecter 已是最新版本 (v4.3.1)！", L"版本检查", MB_OK | MB_ICONINFORMATION);
            break;
        case ID_TRAY_EXIT:
            removeTrayIcon();
            DestroyWindow(hwnd);
            break;
        }
        return 0;
    case WM_CLOSE:
        // 最小化到托盘
        ShowWindow(hwnd, SW_HIDE);
        return 0;
    case WM_DESTROY:
        removeTrayIcon();
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(hwnd, msg, wParam, lParam);
}

} // namespace

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE, PWSTR pCmdLine, int nCmdShow) {
    appInstance = hInstance;

    // 单实例互斥检查
    HANDLE mutex = CreateMutexW(nullptr, TRUE, L"CollecterStandaloneMainWindowMutex");
    if (mutex != nullptr && GetLastError() == ERROR_ALREADY_EXISTS) {
        HWND existing = FindWindowW(L"CollecterStandaloneMainWindow", nullptr);
        if (existing) {
            ShowWindow(existing, SW_RESTORE);
            SetForegroundWindow(existing);
        }
        CloseHandle(mutex);
        return 0;
    }

    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");

    WNDCLASSEXW wcex{};
    wcex.cbSize = sizeof(WNDCLASSEXW);
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = WndProc;
    wcex.hInstance = hInstance;
    wcex.hIcon = LoadIcon(hInstance, MAKEINTRESOURCE(1));
    applicationIcon = wcex.hIcon;
    wcex.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wcex.hbrBackground = reinterpret_cast<HBRUSH>(COLOR_WINDOW + 1);
    wcex.lpszClassName = L"CollecterStandaloneMainWindow";
    RegisterClassExW(&wcex);

    windowHandle = CreateWindowW(
        L"CollecterStandaloneMainWindow",
        L"Collecter — 资产与收纳管家",
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, 1200, 800,
        nullptr, nullptr, hInstance, nullptr
    );

    if (!windowHandle) return 1;

    registerTrayIcon(windowHandle);
    ShowWindow(windowHandle, nCmdShow);
    UpdateWindow(windowHandle);

    // 初始化 WebView2
    CreateCoreWebView2EnvironmentWithOptions(nullptr, nullptr, nullptr,
        Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
            [windowHandle](HRESULT result, ICoreWebView2Environment* env) -> HRESULT {
                if (FAILED(result) || !env) return result;
                sharedEnvironment = env;
                env->CreateCoreWebView2Controller(windowHandle,
                    Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                        [](HRESULT res, ICoreWebView2Controller* c) -> HRESULT {
                            if (FAILED(res) || !c) return res;
                            controller = c;
                            controller->get_CoreWebView2(&webview);
                            updateBounds();

                            webview->add_NavigationStarting(
                                Callback<ICoreWebView2NavigationStartingEventHandler>(
                                    [](ICoreWebView2*, ICoreWebView2NavigationStartingEventArgs* args) -> HRESULT {
                                        LPWSTR uri = nullptr;
                                        args->get_Uri(&uri);
                                        if (uri) {
                                            if (!isAllowedUrl(uri)) {
                                                args->put_Cancel(TRUE);
                                                ShellExecuteW(nullptr, L"open", uri, nullptr, nullptr, SW_SHOWNORMAL);
                                            }
                                            CoTaskMemFree(uri);
                                        }
                                        return S_OK;
                                    }).Get(), nullptr);

                            webview->NavigateToString(loadingPageHtml());
                            SetTimer(windowHandle, ID_PROBE_TIMER, 500, nullptr);
                            return S_OK;
                        }).Get());
                return S_OK;
            }).Get());

    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (mutex) {
        ReleaseMutex(mutex);
        CloseHandle(mutex);
    }

    return static_cast<int>(msg.wParam);
}
