graph TB
subgraph "📱 UI Layer (Presentation)"
MA[MainActivity.kt]
BS[BrowserScreen.kt]

        subgraph "🧩 UI Components"
            BTC[BrowserTopBar.kt]
            BBC[BrowserBottomBar.kt]
            BSD[BrowserSettingsDialog.kt]
            BPI[BrowserProgressIndicator.kt]
            SP[StartPage.kt]
        end
        
        subgraph "🎨 Theme & Design"
            THM[Theme.kt]
            COL[Color.kt]
            TYP[Typography.kt]
            BAR[BarTokens.kt]
        end
        
        subgraph "🌐 WebView Integration"
            WVH[WebViewHost.kt]
            FUH[FileUploadHandler.kt]
            DH[DownloadHandler.kt]
        end
    end
    
    subgraph "🧠 Presentation Layer (MVI)"
        BVM[BrowserViewModel.kt]
        BST[BrowserState.kt]
        BIN[BrowserIntent.kt]
        BEF[BrowserEffect.kt]
        BRD[BrowserReducer.kt]
    end
    
    subgraph "📋 Domain Layer"
        subgraph "🔧 Settings Domain"
            BSR[BrowserSettingsRepository.kt]
            WVC[WebViewConfig.kt]
            NS[NavigationState.kt]
        end
        
        subgraph "🌍 Core Models"
            VU[ValidatedUrl.kt]
        end
    end
    
    subgraph "💾 Data Layer"
        subgraph "⚙️ Settings Data"
            BSRI[BrowserSettingsRepositoryImpl.kt]
            BSS[BrowserSettingsSerializer.kt]
        end
    end
    
    subgraph "🌐 Network Layer"
        subgraph "🔗 HTTP Engines"
            OHE[OkHttpEngine.kt]
            OHS[OkHttpStack.kt]
            CHS[CronetHttpStack.kt]
            CH[CronetHolder.kt]
            HSF[HttpStackFactory.kt]
            HS[HttpStack.kt]
        end
        
        subgraph "🕷️ User Agent Management"
            UAP[UserAgentProvider.kt]
            UACHM[UserAgentClientHintsManager.kt]
            UAPV[UAProvider.kt]
        end
        
        subgraph "🔄 Network Utilities"
            NP[NetworkProxy.kt]
            PV[ProxyValidator.kt]
            PST[ProxySmokeTest.kt]
        end
    end
    
    subgraph "🌐 WebView Layer"
        BWC[BrowserWebViewClient.kt]
        
        subgraph "📜 JavaScript Integration"
            JSB[JsBridge.kt]
            CCI[ChromeCompatibilityInjector.kt]
            JCSP[JsCompatScriptProvider.kt]
        end
        
        subgraph "🔒 Headers & Security"
            RWHM[RequestedWithHeaderMode.kt]
        end
    end
    
    subgraph "⚙️ Settings Layer"
        DS[DeveloperSettings.kt]
    end
    
    subgraph "🔧 Dependency Injection"
        BA[BrowserApp.kt]
        AM[AppModule.kt]
        BM[BrowserModule.kt]
        SM[SettingsModule.kt]
        CM[CoreModule.kt]
    end
    
    MA --> BS
    BS --> BTC
    BS --> BBC
    BS --> BSD
    BS --> BPI
    BS --> SP
    BS --> WVH
    BS --> BVM
    BVM --> BST
    BVM --> BIN
    BVM --> BEF
    BVM --> BRD
    BRD --> BST
    BIN --> BRD
    BRD --> BEF
    BVM --> BSR
    BSR --> WVC
    BVM --> VU
    BSR --> BSRI
    BSRI --> BSS
    WVH --> BWC
    BWC --> JSB
    BWC --> OHE
    JSB --> UAPV
    OHE --> UAP
    OHE --> DS
    HSF --> OHS
    HSF --> CHS
    CHS --> CH
    CCI --> JCSP
    JSB --> CCI
    BA --> AM
    BA --> SM
    BA --> CM
    AM --> BM
    BSD --> DS
    DS --> WVC
    WVH --> FUH
    WVH --> DH
    OHE --> NP
    NP --> PV
    PV --> PST
    UAPV --> UAP
    UAP --> UACHM
    BS --> THM
    THM --> COL
    THM --> TYP
    THM --> BAR

    classDef uiLayer fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef presentationLayer fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef domainLayer fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef dataLayer fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef networkLayer fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    classDef webviewLayer fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    classDef diLayer fill:#e0f2f1,stroke:#004d40,stroke-width:2px
    classDef settingsLayer fill:#fafafa,stroke:#424242,stroke-width:2px
    
    class MA,BS,BTC,BBC,BSD,BPI,SP,WVH,FUH,DH,THM,COL,TYP,BAR uiLayer
    class BVM,BST,BIN,BEF,BRD presentationLayer
    class BSR,WVC,NS,VU domainLayer
    class BSRI,BSS dataLayer
    class OHE,OHS,CHS,CH,HSF,HS,UAP,UACHM,UAPV,NP,PV,PST networkLayer
    class BWC,JSB,CCI,JCSP,RWHM webviewLayer
    class BA,AM,BM,SM,CM diLayer
    class DS settingsLayer
