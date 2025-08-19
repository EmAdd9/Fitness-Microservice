
export const authConfig =  {
    clientId: 'fitness-oauth2-pkce',
    authorizationEndpoint: 'http://localhost:8181/realms/fitness-oauth2/protocol/openid-connect/auth/',
    tokenEndpoint: 'http://localhost:8181/realms/fitness-oauth2/protocol/openid-connect/token',
    redirectUri: 'http://localhost:5173',
    scope: 'openid email profile offline_access',
    onRefreshTokenExpire: (event) => event.login(),
}