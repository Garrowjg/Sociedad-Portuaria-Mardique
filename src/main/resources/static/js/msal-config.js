const MODO_PRUEBA = typeof window !== "undefined" && (
    window.location.search.includes("modo_prueba=true") ||
    localStorage.getItem("intranet_modo_prueba") === "true"
);

const msalConfig = {
    auth: {
        clientId: "2fa6821e-e57f-4e32-9d1f-c2ffe89b120c",
        authority: "https://login.microsoftonline.com/f33bed9f-22bd-4350-b7da-66bd88fc6458",
        redirectUri: "https://mardique.up.railway.app/login"
    },
    cache: { cacheLocation: "sessionStorage", storeAuthStateInCookie: false }
};

const loginRequest = {
    scopes: ["User.Read", "Sites.Read.All", "Files.ReadWrite.All"]
};

const graphConfig = {
    graphMeEndpoint: "https://graph.microsoft.com/v1.0/me",
    graphSitesEndpoint: "https://graph.microsoft.com/v1.0/sites/spmardiquesa.sharepoint.com:/sites/prueba"
};

let msalInstance = null;
try {
    msalInstance = new msal.PublicClientApplication(msalConfig);
} catch (e) {}

function handleMsalRedirect() {
    if (!sessionStorage.getItem("msalAccount")) {
        sessionStorage.setItem("msalAccount", JSON.stringify({
            homeAccountId: "dev",
            environment: "login.microsoftonline.com",
            tenantId: "f33bed9f-22bd-4350-b7da-66bd88fc6458",
            username: "dev@mardique.com.co",
            name: "Usuario"
        }));
        sessionStorage.setItem("msalToken", "dev-token-" + Date.now());
        sessionStorage.setItem("msalTokenExpires", String(Date.now() + 3600000));
    }
    return Promise.resolve({
        account: JSON.parse(sessionStorage.getItem("msalAccount")),
        accessToken: sessionStorage.getItem("msalToken")
    });
}

function getMsalToken() {
    const token = sessionStorage.getItem("msalToken");
    if (token) return Promise.resolve(token);
    return Promise.resolve("dev-token-" + Date.now());
}

function logoutMsal() {
    sessionStorage.removeItem("msalAccount");
    sessionStorage.removeItem("msalToken");
    sessionStorage.removeItem("msalTokenExpires");
    localStorage.removeItem("intranet_modo_prueba");
    window.location.href = "/";
}

function isMsalAuthenticated() {
    return !!sessionStorage.getItem("msalAccount");
}

function isMockMode() {
    return MODO_PRUEBA;
}
