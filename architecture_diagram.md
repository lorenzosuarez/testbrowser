# Project Architecture

```mermaid
graph TD
    %% Application Layer
    subgraph App["🚀 Application"]
        BrowserApp["BrowserApp<br/>(Application)"]
        MainActivity["MainActivity<br/>(Activity)"]
    end

    %% Dependency Injection Layer
    subgraph DI["🔧 Dependency Injection"]
        AppModule["AppModule"]
        CoreModule["CoreModule"]
        SettingsModule["SettingsModule"]
        BrowserModule["BrowserModule"]
    end

    %% Presentation Layer (MVI)
    subgraph Presentation["🎨 Presentation (MVI)"]
        BrowserViewModel["BrowserViewModel"]
        BrowserState["BrowserState"]
        BrowserIntent["BrowserIntent"]
        BrowserEffect["BrowserEffect"]
        BrowserReducer["BrowserReducer"]
        BrowserMode["BrowserMode"]
    end

    %% Domain Layer
    subgraph Domain["🏛️ Domain"]
        BrowserSettingsRepository["BrowserSettingsRepository<br/>(Interface)"]
        WebViewConfig["WebViewConfig<br/>(Data Class)"]
        AcceptLanguageMode["AcceptLanguageMode<br/>(Enum)"]
        EngineMode["EngineMode<br/>(Enum)"]
    end

    %% Data Layer
    subgraph Data["💾 Data"]
        BrowserSettingsRepositoryImpl["BrowserSettingsRepositoryImpl"]
        DataStore["DataStore<Preferences>"]
        BrowserSettingsSerializer["BrowserSettingsSerializer"]
    end

    %% Core Components
    subgraph Core["⚡ Core"]
        ValidatedUrl["ValidatedUrl"]
        DeveloperSettings["DeveloperSettings"]
        UserAgentClientHintsManager["UserAgentClientHintsManager"]
    end

    %% Network Layer
    subgraph Network["🌐 Network"]
        HttpStack["HttpStack<br/>(Interface)"]
        OkHttpStack["OkHttpStack"]
        CronetHttpStack["CronetHttpStack"]
        HttpStackFactory["HttpStackFactory"]
        OkHttpClientProvider["OkHttpClientProvider"]
        CronetHolder["CronetHolder"]
        ProxyRequest["ProxyRequest"]
        ProxyResponse["ProxyResponse"]
        UserAgentProvider["UserAgentProvider"]
        NetworkProxy["NetworkProxy<br/>(Interface)"]
        DefaultNetworkProxy["DefaultNetworkProxy"]
    end

    %% UI Layer
    subgraph UI["🖥️ UI Components"]
        BrowserScreen["BrowserScreen"]
        BrowserTopBar["BrowserTopBar"]
        BrowserBottomBar["BrowserBottomBar"]
        BrowserProgressIndicator["BrowserProgressIndicator"]
        BrowserSettingsDialog["BrowserSettingsDialog"]
        StartPage["StartPage"]
        WebViewHost["WebViewHost"]
    end

    %% Browser Components
    subgraph Browser["🌍 Browser Components"]
        UAProvider["UAProvider<br/>(Interface)"]
        ChromeUAProvider["ChromeUAProvider"]
        VersionProvider["VersionProvider<br/>(Interface)"]
        AndroidVersionProvider["AndroidVersionProvider"]
        WebViewController["WebViewController"]
        WebViewDebug["WebViewDebug"]
        FileUploadHandler["FileUploadHandler"]
        DownloadHandler["DownloadHandler"]
        ProxyValidator["ProxyValidator"]
        RequestedWithHeaderMode["RequestedWithHeaderMode"]
        ChromeCompatibilityInjector["ChromeCompatibilityInjector"]
        JsCompatScriptProvider["JsCompatScriptProvider<br/>(Interface)"]
    end

    %% WebView Layer
    subgraph WebView["📱 WebView"]
        BrowserWebViewClient["BrowserWebViewClient"]
    end

    %% JavaScript Bridge
    subgraph JS["📜 JavaScript"]
        JsBridge["JsBridge"]
    end

    %% Theme System
    subgraph Theme["🎨 Theme"]
        StatusBarGradient["StatusBarGradient"]
        Color["Color"]
        Typography["Typography"]
        Theme["Theme"]
        BarTokens["BarTokens"]
        OmniboxTokens["OmniboxTokens"]
    end

    %% Application Dependencies
    BrowserApp --> AppModule
    BrowserApp --> CoreModule
    BrowserApp --> SettingsModule
    BrowserApp --> BrowserModule
    MainActivity --> BrowserScreen
    MainActivity --> UAProvider

    %% DI Module Provisions
    CoreModule --> DataStore
    CoreModule --> DeveloperSettings
    CoreModule --> UserAgentClientHintsManager
    SettingsModule --> BrowserSettingsRepositoryImpl
    BrowserModule --> BrowserViewModel
    BrowserModule --> AndroidVersionProvider
    BrowserModule --> ChromeUAProvider
    BrowserModule --> JsCompatScriptProvider
    BrowserModule --> DefaultNetworkProxy

    %% Presentation Layer Dependencies
    BrowserViewModel --> BrowserSettingsRepository
    BrowserViewModel --> ValidatedUrl
    BrowserViewModel --> BrowserReducer
    BrowserViewModel --> BrowserState
    BrowserReducer --> ValidatedUrl
    BrowserReducer --> WebViewConfig
    BrowserState --> WebViewConfig
    BrowserState --> ValidatedUrl
    BrowserIntent --> ValidatedUrl
    BrowserIntent --> WebViewConfig
    BrowserEffect --> ValidatedUrl

    %% Data Layer Dependencies
    BrowserSettingsRepositoryImpl --> BrowserSettingsRepository
    BrowserSettingsRepositoryImpl --> DataStore
    BrowserSettingsRepositoryImpl --> WebViewConfig
    BrowserSettingsRepositoryImpl --> AcceptLanguageMode
    BrowserSettingsRepositoryImpl --> EngineMode
    BrowserSettingsRepositoryImpl --> RequestedWithHeaderMode

    %% Network Layer Dependencies
    HttpStackFactory --> DeveloperSettings
    HttpStackFactory --> UAProvider
    HttpStackFactory --> UserAgentClientHintsManager
    HttpStackFactory --> CronetHttpStack
    HttpStackFactory --> OkHttpStack
    OkHttpStack --> HttpStack
    OkHttpStack --> UAProvider
    OkHttpStack --> UserAgentClientHintsManager
    CronetHttpStack --> HttpStack
    CronetHttpStack --> UAProvider
    CronetHttpStack --> UserAgentClientHintsManager
    CronetHttpStack --> CronetHolder
    DefaultNetworkProxy --> NetworkProxy
    DefaultNetworkProxy --> WebViewConfig
    DefaultNetworkProxy --> UAProvider
    DefaultNetworkProxy --> UserAgentClientHintsManager
    DefaultNetworkProxy --> CronetHolder
    DefaultNetworkProxy --> OkHttpStack
    DefaultNetworkProxy --> CronetHttpStack

    %% Browser Component Dependencies
    ChromeUAProvider --> UAProvider
    ChromeUAProvider --> VersionProvider
    AndroidVersionProvider --> VersionProvider
    JsBridge --> UAProvider

    %% WebView Dependencies
    BrowserWebViewClient --> NetworkProxy
    BrowserWebViewClient --> JsBridge
    BrowserWebViewClient --> UAProvider

    %% UI Dependencies
    BrowserScreen --> BrowserViewModel
    BrowserScreen --> BrowserIntent
    BrowserScreen --> BrowserEffect
    BrowserScreen --> BrowserSettingsDialog
    BrowserScreen --> BrowserTopBar
    BrowserScreen --> BrowserBottomBar
    BrowserScreen --> StartPage
    BrowserScreen --> BrowserProgressIndicator
    BrowserScreen --> WebViewHost
    BrowserScreen --> UAProvider
    BrowserScreen --> JsCompatScriptProvider
    BrowserSettingsDialog --> WebViewConfig
    WebViewHost --> BrowserWebViewClient
    WebViewHost --> WebViewConfig
    WebViewHost --> JsBridge

    %% Theme Dependencies
    StatusBarGradient --> WebViewConfig
    BrowserScreen --> Theme
    BrowserTopBar --> BarTokens
    BrowserBottomBar --> BarTokens

    %% Core Dependencies
    UserAgentClientHintsManager --> DeveloperSettings

    %% Data Flow Indicators
    classDef appLayer fill:#e1f5fe
    classDef diLayer fill:#f3e5f5
    classDef presentationLayer fill:#e8f5e8
    classDef domainLayer fill:#fff3e0
    classDef dataLayer fill:#fce4ec
    classDef coreLayer fill:#f1f8e9
    classDef networkLayer fill:#e0f2f1
    classDef uiLayer fill:#e3f2fd
    classDef browserLayer fill:#fff8e1
    classDef webviewLayer fill:#fafafa
    classDef jsLayer fill:#f9fbe7
    classDef themeLayer fill:#fce4ec

    class BrowserApp,MainActivity appLayer
    class AppModule,CoreModule,SettingsModule,BrowserModule diLayer
    class BrowserViewModel,BrowserState,BrowserIntent,BrowserEffect,BrowserReducer,BrowserMode presentationLayer
    class BrowserSettingsRepository,WebViewConfig,AcceptLanguageMode,EngineMode domainLayer
    class BrowserSettingsRepositoryImpl,DataStore,BrowserSettingsSerializer dataLayer
    class ValidatedUrl,DeveloperSettings,UserAgentClientHintsManager coreLayer
    class HttpStack,OkHttpStack,CronetHttpStack,HttpStackFactory,OkHttpClientProvider,CronetHolder,ProxyRequest,ProxyResponse,UserAgentProvider,NetworkProxy,DefaultNetworkProxy networkLayer
    class BrowserScreen,BrowserTopBar,BrowserBottomBar,BrowserProgressIndicator,BrowserSettingsDialog,StartPage,WebViewHost uiLayer
    class UAProvider,ChromeUAProvider,VersionProvider,AndroidVersionProvider,WebViewController,WebViewDebug,FileUploadHandler,DownloadHandler,ProxyValidator,RequestedWithHeaderMode,ChromeCompatibilityInjector,JsCompatScriptProvider browserLayer
    class BrowserWebViewClient webviewLayer
    class JsBridge jsLayer
    class StatusBarGradient,Color,Typography,Theme,BarTokens,OmniboxTokens themeLayer
```
````
