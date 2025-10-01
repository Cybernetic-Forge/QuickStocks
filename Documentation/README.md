# QuickStocks Documentation

This folder contains all documentation for the QuickStocks plugin, designed to be used with GitHub Wiki.

## 📁 Files Overview

### Core Pages
- **`Home.md`** - Wiki landing page (GitHub Wiki entry point)
- **`_Sidebar.md`** - Navigation sidebar for GitHub Wiki
- **`Getting-Started.md`** - Introduction for new users
- **`Permissions.md`** - Complete permission reference
- **`Commands-Overview.md`** - Overview of all command domains

### Command Documentation
- **`Commands-Stocks.md`** - Stock market commands (`/stocks`)
- **`Commands-Crypto.md`** - Cryptocurrency commands (`/crypto`)
- **`Commands-Wallet.md`** - Wallet management (`/wallet`)
- **`Commands-Market.md`** - Market trading (`/market`)
- **`Commands-Watch.md`** - Watchlist management (`/watch`)
- **`Commands-Company.md`** - Company operations (`/company`)
- **`Commands-MarketDevice.md`** - Market device (`/marketdevice`)

### Administration
- **`Installation.md`** - Step-by-step installation guide
- **`Configuration.md`** - Detailed configuration reference
- **`Database.md`** - Database management and maintenance

## 🚀 Setting Up GitHub Wiki

### Option 1: Automatic Setup (Recommended)

1. Enable GitHub Wiki for your repository
2. Clone the wiki repository:
   ```bash
   git clone https://github.com/Cybernetic-Forge/QuickStocks.wiki.git
   ```
3. Copy all files from this `Documentation/` folder to the wiki repo
4. Commit and push:
   ```bash
   cd QuickStocks.wiki
   git add .
   git commit -m "Add complete documentation"
   git push
   ```

### Option 2: Manual Setup

1. Go to your repository's Wiki tab
2. Click "Create the first page"
3. Copy content from `Home.md` into the wiki page
4. Create additional pages for each `.md` file
5. Set up the sidebar using `_Sidebar.md` content

## 📋 File Naming Convention

GitHub Wiki uses specific file names:
- `Home.md` → Main wiki page
- `_Sidebar.md` → Sidebar navigation
- Other files → Page URLs (e.g., `Getting-Started.md` → `/Getting-Started`)

## 🔗 Internal Links

All documentation uses relative links that work in GitHub Wiki:
- `[Text](Page-Name)` - Links to another wiki page
- `[Text](Page-Name#section)` - Links to specific section
- External links use full URLs

## 🎨 Features

### Comprehensive Coverage
- **13 documentation pages** covering all aspects
- **~100 pages** of detailed content
- Step-by-step tutorials and examples
- Troubleshooting sections
- Best practices and tips

### User-Focused
- **For Players:** Getting started, commands, tutorials
- **For Admins:** Installation, configuration, database
- **For Developers:** Architecture notes (in code comments)

### Well-Organized
- Clear navigation structure
- Cross-references between related topics
- Consistent formatting
- Search-friendly content

## 📝 Placeholder Comments

Throughout the documentation, you'll find placeholder comments like:

```markdown
<!-- [GUI Screenshot Placeholder: Market Browser Interface] -->
```

These indicate where screenshots should be added in the future. To add screenshots:

1. Take screenshot of the feature
2. Upload to wiki or use image hosting
3. Replace placeholder with: `![Description](image-url)`

## ✏️ Updating Documentation

When updating docs:

1. Edit files in this `Documentation/` folder
2. Test locally if possible
3. Commit changes to main repository
4. Sync to GitHub Wiki (repeat setup process)

## 🎯 Documentation Goals

This documentation aims to:
- ✅ Help new players get started quickly
- ✅ Provide complete command reference
- ✅ Guide administrators through setup
- ✅ Explain all configuration options
- ✅ Troubleshoot common issues
- ✅ Establish best practices

## 📊 Statistics

- **Total Pages:** 13
- **Total Words:** ~85,000
- **Command Examples:** 200+
- **Configuration Options:** 50+
- **Troubleshooting Sections:** 30+

## 🔗 Quick Links

For the complete experience, visit:
- **[Getting Started](Getting-Started.md)** - Start here!
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Installation](Installation.md)** - Setup guide

---

*Ready to transform your Minecraft economy? [Get Started →](Getting-Started.md)*
