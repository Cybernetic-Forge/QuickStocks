# Market Device Commands

The `/marketdevice` command allows administrators to grant special Market Link Device items that provide portable access to the trading interface.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/marketdevice give [player]` | Give Market Link Device | `maksy.stocks.marketdevice.give` |

**Aliases:** `/mdevice`

---

## 📱 What is a Market Link Device?

The **Market Link Device** is a special item that players can carry in their inventory to access the market interface on-the-go.

**Features:**
- **Portable market access** - Use anywhere, anytime
- **Right-click to open** - Opens the market GUI
- **Player-bound** - Tied to the receiving player
- **Persistent** - Keeps working after server restarts
- **Special item** - Unique appearance and lore

**Appearance:**
- **Item:** Ender Eye (glowing)
- **Name:** "Market Link Device" (colored)
- **Lore:** Instructions and owner information
- **Enchantment Glint:** Yes (looks special)

---

## 🎁 Giving Market Devices

### `/marketdevice give [player]`

Grant a Market Link Device item to a player.

**Permission:** `maksy.stocks.marketdevice.give` (operators only)

**Arguments:**
- `[player]` - Target player name (optional - gives to yourself if omitted)

**Examples:**
```
/marketdevice give Steve
/marketdevice give          # Give to yourself
/mdevice give Alex          # Using alias
```

**Sample Output (to sender):**
```
✅ Market Link Device given to Steve
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
The device has been added to Steve's inventory.
They can right-click it to access the market.
```

**Sample Output (to recipient):**
```
📱 You received a Market Link Device!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Right-click this item to access the market
interface from anywhere!

Item added to your inventory.
```

**Item Received:**
```
📱 Market Link Device
   
   Right-click to access the market
   Owner: Steve
   Version: 1.0
   
   Trade stocks and company shares
   from anywhere in the world!
```

---

## 🎮 Using the Market Device

### For Players

**How to use:**
1. Hold the Market Link Device in your hand
2. Right-click while holding it
3. Market GUI opens instantly
4. Browse, buy, and sell as usual

**Quick Access:**
```
Right-click device → Market opens → Trade → Done!
```

**Alternative Methods:**
- Use `/market` command (works anywhere)
- Visit market locations (if configured by admins)

### Device Features

**Portable:**
- Works anywhere in any world
- No need to visit specific locations
- Instant market access

**Persistent:**
- Survives server restarts
- Keeps working after logout
- Won't disappear

**Personal:**
- Bound to receiving player
- Includes owner name in lore
- Version tracked

---

## 👑 Administrative Use

### When to Give Devices

**Good Use Cases:**
1. **VIP/Donator Perks** - Premium feature for supporters
2. **Event Rewards** - Prize for trading competitions
3. **Staff Tools** - Give to moderators for testing
4. **Achievement Rewards** - Earn through accomplishments
5. **Special Events** - Limited-time distribution

**Example Scenarios:**
```bash
# Reward event winner
/marketdevice give WinnerPlayer

# Give to VIP members
/marketdevice give VIPPlayer1
/marketdevice give VIPPlayer2

# Give to staff for testing
/marketdevice give Moderator
```

### Giving to Multiple Players

```bash
# Give to all online VIPs
for player in VIP1 VIP2 VIP3
do
  /marketdevice give $player
done
```

### Giving to Yourself (Testing)

```bash
# Test the device
/marketdevice give
# Right-click to test functionality
```

---

## 🔧 Configuration Options

### Crafting Recipe

The Market Device can be made craftable via config.yml:

```yaml
marketDevice:
  recipe:
    enabled: false        # Enable crafting recipe
    shapedRecipe: true   # Use 3x3 shaped recipe
```

**Crafting Recipe (if enabled):**
```
[E] [D] [E]
[G] [C] [G]
[E] [R] [E]

E = Ender Pearl
D = Diamond
G = Gold Ingot
C = Compass
R = Redstone Block
```

**Note:** Crafting is disabled by default to keep devices special.

---

## 💡 Use Cases and Ideas

### Server Economy

**Premium Feature:**
- Sell devices in server shop
- Make available to donors
- Reward for achievements
- Trading competition prizes

**Example Implementation:**
```
- Device costs $50,000 in shop
- VIP rank includes one device
- Win trading competition → get device
- Reach $1M portfolio → earn device
```

### Events and Competitions

**Trading Competitions:**
- All participants get devices
- Easier access during event
- Remove after event (or keep as prize)

**Example:**
```bash
# Event start - give devices
/marketdevice give Trader1
/marketdevice give Trader2
# ... all participants

# Event ends - keep as reward!
```

### Role-Playing Servers

**Story Integration:**
- "Advanced trading technology"
- "Merchant guild membership"
- "Wireless market access"
- "Futuristic trading tool"

### Educational Servers

**Teaching Tool:**
- Give to economics students
- Practice trading anywhere
- Easy access for learning
- Remove barriers to entry

---

## 🎯 Common Scenarios

### Scenario 1: VIP Perk

```bash
# Player purchases VIP rank
"Thank you for supporting the server!"

# Admin gives device
/marketdevice give NewVIPPlayer

# Player uses it
NewVIPPlayer: Right-clicks device
System: Opens market GUI
NewVIPPlayer: "Wow, so convenient!"
```

### Scenario 2: Event Prize

```bash
# Trading competition ends
/company info TopTrader
# Portfolio: $500,000 (+200%)

# Award winner
/marketdevice give TopTrader
"Congratulations! Here's your prize!"

# Winner receives device
"Thanks! This is awesome!"
```

### Scenario 3: Staff Testing

```bash
# Developer testing new features
/marketdevice give Developer

# Test market access
Developer: Right-clicks device
System: Market opens

# Verify functionality
"Working perfectly!"
```

### Scenario 4: Achievement System

```bash
# Player reaches milestone
/market portfolio
# Portfolio Value: $1,000,000

# Automatic reward
/marketdevice give MillionairePlayer
"Achievement Unlocked: Millionaire Status!"
```

---

## 💡 Tips and Best Practices

### For Administrators

1. **Keep it special** - Don't give to everyone
2. **Track distribution** - Know who has devices
3. **Set clear criteria** - Define how to earn one
4. **Consider alternatives** - `/market` command works too
5. **Test before distributing** - Make sure it works
6. **Document your system** - Tell players how to get one

### For Server Owners

1. **Monetization option** - Sell in donation shop
2. **Engagement tool** - Reward active traders
3. **VIP exclusive** - Make it premium feature
4. **Event tool** - Use for special occasions
5. **Balance with access** - Don't make `/market` command worse

### Device Management

**Keep Track:**
- Who has devices
- Why they received them
- When they were given
- Any special conditions

**Set Rules:**
- Can they be traded?
- Are they temporary?
- Can you lose them?
- Are they account-bound?

---

## 🆘 Troubleshooting

### "You don't have permission to give Market Link Devices"

**Cause:** Missing `maksy.stocks.marketdevice.give` permission

**Solution:**
- Only operators can use this command
- Request operator status
- Use permission plugin to grant permission

### "Player not found"

**Cause:** Target player is offline or name misspelled

**Solution:**
- Check player name spelling
- Verify player is online
- Use exact capitalization

### Device doesn't work when right-clicked

**Cause:** Item metadata corrupted or plugin issue

**Solution:**
- Give a new device
- Check server logs for errors
- Verify plugin is updated
- Restart server if needed

### Lost device item

**Cause:** Dropped, died, or accidentally destroyed

**Solution:**
- Give a new device: `/marketdevice give`
- Players should keep it safe
- Consider making it unstackable
- Could enable crafting as backup

### Too many players have devices

**Cause:** Given out too freely

**Solution:**
- Communicate devices are special
- Set stricter criteria for earning
- Consider removing from some players
- Re-evaluate distribution policy

---

## 🔐 Permission Setup

### Using LuckPerms

**Grant to administrators:**
```
luckperms group admin permission set maksy.stocks.marketdevice.give true
```

**Grant to specific moderator:**
```
luckperms user ModeratorName permission set maksy.stocks.marketdevice.give true
```

### Using PermissionsEx

```
pex group admin add maksy.stocks.marketdevice.give
pex user ModeratorName add maksy.stocks.marketdevice.give
```

### Configuration

**In permissions.yml or similar:**
```yaml
groups:
  admin:
    permissions:
      - maksy.stocks.marketdevice.give
  moderator:
    permissions:
      - maksy.stocks.marketdevice.give
```

---

## 🔗 Related Commands

- **[`/market`](Commands-Market.md)** - Access market (alternative to device)
- **[`/wallet`](Commands-Wallet.md)** - Check funds for trading
- **[`/stocks`](Commands-Stocks.md)** - View stock information

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Plugin basics
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Market Commands](Commands-Market.md)** - Trading guide
- **[Permissions](Permissions.md)** - Permission reference

---

*For server configuration, see [Configuration](Configuration.md)*
