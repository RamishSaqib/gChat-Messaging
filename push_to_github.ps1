# PowerShell script to initialize and push gChat to GitHub
# Run this after creating the repository on GitHub

Write-Host "🚀 gChat GitHub Push Script" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# Check if git is installed
try {
    $gitVersion = git --version
    Write-Host "✅ Git found: $gitVersion`n" -ForegroundColor Green
} catch {
    Write-Host "❌ Git not found. Please install Git first." -ForegroundColor Red
    exit 1
}

# Get GitHub username
$username = Read-Host "Enter your GitHub username"

if ([string]::IsNullOrWhiteSpace($username)) {
    Write-Host "❌ Username cannot be empty" -ForegroundColor Red
    exit 1
}

Write-Host "`n📋 Repository will be: https://github.com/$username/gChat-Messaging`n" -ForegroundColor Yellow

# Confirm
$confirm = Read-Host "Have you created 'gChat-Messaging' repository on GitHub? (y/n)"
if ($confirm -ne 'y') {
    Write-Host "`n⚠️  Please create the repository first at: https://github.com/new" -ForegroundColor Yellow
    Write-Host "   Repository name: gChat-Messaging" -ForegroundColor Yellow
    Write-Host "   Do NOT initialize with README, .gitignore, or license`n" -ForegroundColor Yellow
    exit 0
}

Write-Host "`n🔄 Initializing git repository..." -ForegroundColor Cyan
git init

Write-Host "📦 Adding files (respecting .gitignore)..." -ForegroundColor Cyan
git add .

Write-Host "💾 Creating initial commit..." -ForegroundColor Cyan
git commit -m "Initial commit: gChat MVP with Firebase auth, messaging, and Google Sign-In

Features:
- Email and Google Sign-In authentication
- Real-time messaging with offline support
- Clean Architecture (Domain, Data, Presentation)
- Firebase integration (Auth, Firestore, FCM)
- Material 3 UI with Jetpack Compose
- Room database for local persistence
- Hilt dependency injection with KSP
- Ready for AI translation features"

Write-Host "🔗 Adding remote origin..." -ForegroundColor Cyan
git remote add origin "https://github.com/$username/gChat-Messaging.git"

Write-Host "⬆️  Pushing to GitHub..." -ForegroundColor Cyan
git branch -M main
git push -u origin main

Write-Host "`n✅ Successfully pushed to GitHub!" -ForegroundColor Green
Write-Host "🌐 View at: https://github.com/$username/gChat-Messaging`n" -ForegroundColor Green

Write-Host "📋 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Add repository description and topics on GitHub" -ForegroundColor White
Write-Host "   2. Verify no secrets were pushed (check google-services.json is NOT there)" -ForegroundColor White
Write-Host "   3. Add collaborators if needed" -ForegroundColor White
Write-Host "   4. Consider adding a LICENSE file`n" -ForegroundColor White

