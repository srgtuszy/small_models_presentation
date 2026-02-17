#!/usr/bin/env python3
"""
Generate expanded training dataset for the tiny transformer.

This script creates diverse training examples for 4 command categories:
1. Alert commands - Display messages to the user
2. Navigate commands - Move between screens/pages
3. Toggle commands - Enable/disable settings
4. System commands - Built-in actions (back, refresh, close, etc.)
5. Unrecognized - Non-command inputs (greetings, random text)

Goal: 100+ unique examples per category to improve model generalization.
"""
import random


def generate_random_word_combinations(count=100):
    """Generate random word combinations to teach the model to handle arbitrary text."""
    words = [
        'the', 'a', 'an', 'this', 'that', 'my', 'your', 'our', 'their',
        'new', 'old', 'big', 'small', 'good', 'bad', 'fast', 'slow',
        'message', 'alert', 'notification', 'update', 'info', 'news',
        'system', 'user', 'data', 'file', 'item', 'task', 'job',
        'done', 'ready', 'pending', 'complete', 'failed', 'success',
        'start', 'stop', 'begin', 'end', 'open', 'close', 'save',
        'loading', 'saving', 'processing', 'running', 'waiting',
        'error', 'warning', 'info', 'debug', 'critical', 'urgent',
        'test', 'demo', 'sample', 'example', 'custom', 'special',
        'today', 'now', 'soon', 'later', 'tomorrow', 'yesterday',
        'all', 'some', 'any', 'each', 'every', 'many', 'few', 'more',
        'first', 'last', 'next', 'previous', 'current', 'final',
        'incoming', 'outgoing', 'unread', 'new', 'recent', 'latest'
    ]
    
    combinations = []
    for _ in range(count):
        num_words = random.randint(1, 4)
        combo = ' '.join(random.choice(words) for _ in range(num_words))
        combinations.append(combo)
    return combinations


def generate_dataset():
    """
    Generate training samples and return them as a list.
    
    Each sample is a string in format:
    "INPUT: <natural language command> OUTPUT: <JSON action>"
    
    The model learns to map the natural language input to structured JSON output.
    """
    samples = []

    # ==========================================================================
    # ALERT COMMANDS (~170 samples)
    # ==========================================================================
    # These commands display messages to the user in a popup/notification.
    # The model must extract the exact message text and put it in the JSON.
    # 
    # Pattern: {"action": "alert", "message": "<extracted message>"}
    # ==========================================================================
    
    alert_messages = [
        # Simple status messages
        'Hello', 'Welcome', 'Success', 'Error', 'Warning', 'Test', 'Done', 'Info', 'Note', 'Alert',
        
        # Multi-word common messages
        'Hello world', 'Good morning', 'Task done', 'File saved', 'Loading', 'Please wait', 'Error 404',
        'Connection lost', 'Sync complete', 'Build failed', 'Upload complete', 'Download started',
        
        # Process states
        'Process running', 'Operation failed', 'Access granted', 'Access denied', 'Session expired',
        'Login successful', 'Logout complete', 'Settings saved', 'Data exported', 'Import complete',
        
        # System operations
        'Backup created', 'Restore complete', 'Update available', 'Update installed', 'Restart required',
        'System ready', 'Initializing', 'Processing', 'Completed', 'Cancelled', 'Paused', 'Resumed',
        
        # Action states
        'Timeout', 'Retry', 'Skip', 'Next', 'Previous', 'Continue', 'Stop', 'Start', 'Finish',
        'Ready', 'Busy', 'Offline', 'Online', 'Connected', 'Disconnected', 'Syncing', 'Saving',
        
        # Progress messages
        'Loading data', 'Fetching', 'Computing', 'Analyzing', 'Scanning', 'Searching', 'Found',
        'Not found', 'Empty', 'Full', 'Low battery', 'Charging', 'Critical', 'Urgent', 'Important',
        
        # Notifications
        'Reminder', 'Notification', 'Message sent', 'Message received', 'Call incoming', 'Call ended',
        'Meeting started', 'Meeting ended', 'Task assigned', 'Task completed', 'Deadline approaching',
        
        # New content
        'New message', 'New update', 'New feature', 'Beta available', 'Release notes', 'Changelog',
        'Tip of the day', 'Did you know', 'Help needed', 'Support requested', 'Feedback received',
        
        # Issue tracking
        'Bug reported', 'Issue resolved', 'Ticket created', 'Case closed', 'Order placed',
        
        # E-commerce
        'Order shipped', 'Delivery scheduled', 'Payment received', 'Payment failed', 'Refund processed',
        'Subscription active', 'Subscription expired', 'Trial started', 'Trial ending', 'Upgrade available',
        
        # Promotions
        'Promo code applied', 'Discount active', 'Sale started', 'Sale ended', 'Limited time offer',
        'Stock low', 'Out of stock', 'Back in stock', 'Preorder available', 'Coming soon',
        
        # Featured content
        'Released today', 'Just launched', 'Trending now', 'Popular choice', 'Recommended',
        
        # Calendar/events
        'Weather alert', 'Traffic update', 'Event reminder', 'Calendar update', 'Schedule changed',
        
        # Travel
        'Flight delayed', 'Flight cancelled', 'Gate changed', 'Boarding now', 'Final call',
        'Reservation confirmed', 'Booking complete', 'Check-in available', 'Room ready', 'Checkout required',
        
        # System maintenance
        'Maintenance scheduled', 'Downtime planned', 'Service restored', 'System update', 'Security patch',
        
        # Account/security
        'Password changed', 'Profile updated', 'Account verified', 'Email confirmed', 'Phone verified',
        'Two-factor enabled', 'Recovery setup', 'Privacy updated', 'Terms accepted', 'Consent required',
        
        # Social
        'Friend request', 'New follower', 'Mention received', 'Like received', 'Comment added',
        'Share received', 'Invite sent', 'Invite accepted', 'Group joined', 'Channel subscribed',
        
        # Storage/device
        'Storage full', 'Storage low', 'Cache cleared', 'Data reset', 'Factory reset',
        'App installed', 'App updated', 'App removed', 'Permission required', 'Location access',
        'Camera access', 'Microphone access', 'Contacts access', 'Files access', 'Bluetooth access'
    ]

    # Different ways users might phrase alert commands
    # This teaches the model that these all mean the same thing
    alert_templates = [
        'show alert {}',       # Explicit form
        'alert {}',            # Short form
        'display alert {}',    # Alternative verb
        'popup {}',            # UI-specific term
        'show message {}',     # "message" synonym
        'notify {}',           # "notify" verb
        'show notification {}',# Full form
        'send alert {}',       # "send" perspective
        'display message {}',  # Alternative combination
        'pop up {}'            # Two-word variant
    ]

    for msg in alert_messages:
        # Randomly select a template for variety
        template = random.choice(alert_templates)
        samples.append(f'INPUT: {template.format(msg)} OUTPUT: {{"action": "alert", "message": "{msg}"}}')
    
    # Add arbitrary text patterns to teach the model to extract ANY message
    # These use synthetic patterns with numbers, random words, etc.
    # This is crucial for generalization - the model must learn that any text can be a message
    arbitrary_patterns = [
        'abc', 'xyz', 'testing 123', 'random text', 'some message', 'my custom message',
        'this is a test', 'custom alert', 'user defined', 'any text here', 'your message',
        'whatever you want', 'can be anything', 'freeform text', 'type anything', 'say something',
        'message 1', 'message 2', 'message 3', 'alert text', 'notification content',
        'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
        'foo bar baz', 'hello there', 'good news', 'bad news', 'important update',
        'please read', 'urgent notice', 'breaking news', 'just testing', 'demo message',
        'sample', 'example', 'placeholder', 'insert text', 'type here', 'edit me',
        'alpha', 'beta', 'gamma', 'delta', 'epsilon', 'zeta', 'eta', 'theta',
        'red', 'blue', 'green', 'yellow', 'purple', 'orange', 'black', 'white',
        'cat', 'dog', 'bird', 'fish', 'lion', 'tiger', 'bear', 'wolf', 'fox',
        'apple', 'banana', 'cherry', 'date', 'elderberry', 'fig', 'grape',
        'messages', 'all messages', 'new messages', 'unread messages', 'pending messages'
    ]
    
    for msg in arbitrary_patterns:
        template = random.choice(alert_templates)
        samples.append(f'INPUT: {template.format(msg)} OUTPUT: {{"action": "alert", "message": "{msg}"}}')
    
    # Generate synthetic messages with numbers to teach number handling
    for i in range(50):
        num = random.randint(1, 999)
        synthetic_msg = f'message {num}'
        template = random.choice(alert_templates)
        samples.append(f'INPUT: {template.format(synthetic_msg)} OUTPUT: {{"action": "alert", "message": "{synthetic_msg}"}}')
    
    # Generate random word combinations for maximum diversity
    random_combos = generate_random_word_combinations(150)
    for msg in random_combos:
        template = random.choice(alert_templates)
        samples.append(f'INPUT: {template.format(msg)} OUTPUT: {{"action": "alert", "message": "{msg}"}}')

    # ==========================================================================
    # NAVIGATE COMMANDS (~100 samples)
    # ==========================================================================
    # These commands move the user to different screens/pages in the app.
    # The model must identify the target screen name.
    # 
    # Pattern: {"action": "navigate", "target": "<screen name>"}
    # ==========================================================================
    
    navigate_targets = [
        # Core screens
        'home', 'settings', 'profile', 'dashboard', 'login', 'search', 'about', 'help', 'contact',
        
        # Account screens
        'account', 'preferences', 'privacy', 'security', 'notifications', 'messages', 'chat',
        
        # Social screens
        'friends', 'followers', 'following', 'posts', 'photos', 'videos', 'audio', 'files',
        
        # Content screens
        'documents', 'downloads', 'uploads', 'favorites', 'bookmarks', 'history', 'recent',
        
        # Discovery screens
        'trending', 'explore', 'discover', 'browse', 'categories', 'tags', 'topics', 'channels',
        
        # Community screens
        'groups', 'communities', 'events', 'calendar', 'schedule', 'tasks', 'projects', 'notes',
        
        # Utility screens
        'reminders', 'alarms', 'clock', 'timer', 'stopwatch', 'weather', 'maps', 'location',
        
        # Navigation screens
        'directions', 'navigation', 'traffic', 'transit', 'flights', 'hotels', 'restaurants',
        
        # Local screens
        'shops', 'services', 'businesses', 'reviews', 'ratings', 'recommendations', 'suggestions',
        
        # Shopping screens
        'deals', 'offers', 'coupons', 'rewards', 'points', 'credits', 'balance', 'wallet',
        
        # Transaction screens
        'payments', 'orders', 'cart', 'checkout', 'shipping', 'tracking', 'returns', 'refunds',
        
        # Support screens
        'support', 'feedback', 'report', 'bug', 'issue', 'question', 'answer', 'faq',
        
        # Info screens
        'tutorial', 'guide', 'documentation', 'api', 'developer', 'console', 'admin', 'moderation',
        
        # Analytics screens
        'analytics', 'statistics', 'reports', 'insights', 'metrics', 'performance', 'optimization',
        
        # Config screens
        'configuration', 'setup', 'installation', 'update', 'backup', 'restore', 'sync', 'export',
        
        # Data screens
        'import', 'share', 'invite', 'connect', 'link', 'unlink', 'manage', 'edit', 'delete',
        
        # Archive screens
        'archive', 'trash', 'spam', 'inbox', 'sent', 'drafts', 'outbox', 'queue', 'pending',
        
        # Status screens
        'approved', 'rejected', 'blocked', 'muted', 'hidden', 'visible', 'public', 'private'
    ]

    navigate_templates = [
        'navigate to {}',      # Explicit form
        'go to {}',            # Common phrasing
        'open {}',             # Simple verb
        'switch to {}',        # Context switch
        'jump to {}',          # Quick navigation
        'take me to {}',       # User-centric phrasing
        'show {} screen',      # Screen-specific
        'load {} page',        # Page-specific
        'view {}',             # Simple command
        'visit {}'             # Web-style phrasing
    ]

    for target in navigate_targets:
        template = random.choice(navigate_templates)
        samples.append(f'INPUT: {template.format(target)} OUTPUT: {{"action": "navigate", "target": "{target}"}}')

    # ==========================================================================
    # TOGGLE COMMANDS (~100 samples)
    # ==========================================================================
    # These commands enable/disable settings or features.
    # The model must identify the setting name to toggle.
    # 
    # Pattern: {"action": "toggle", "setting": "<setting name>"}
    # ==========================================================================
    
    toggle_settings = [
        # Connectivity
        'dark_mode', 'notifications', 'wifi', 'bluetooth', 'airplane_mode', 'location', 'gps',
        
        # Hardware
        'camera', 'microphone', 'speaker', 'volume', 'brightness', 'auto_rotate', 'screen_lock',
        
        # Security
        'fingerprint', 'face_id', 'password', 'pin', 'two_factor', 'auto_backup', 'auto_sync',
        
        # Auto features
        'auto_update', 'auto_play', 'auto_download', 'auto_connect', 'auto_reply', 'auto_forward',
        'auto_delete', 'auto_archive', 'auto_sort', 'auto_fill', 'auto_correct', 'auto_capitalize',
        'auto_punctuate', 'auto_format', 'auto_save', 'auto_lock', 'auto_brightness', 'auto_theme',
        
        # Do not disturb modes
        'do_not_disturb', 'quiet_hours', 'focus_mode', 'bedtime_mode', 'reading_mode', 'night_mode',
        
        # Display modes
        'eye_comfort', 'blue_light_filter', 'color_correction', 'high_contrast', 'large_text',
        'bold_text', 'reduce_motion', 'reduce_transparency', 'voice_over', 'talk_back', 'switch_access',
        
        # Accessibility
        'select_to_speak', 'braille_keyboard', 'audio_description', 'captions', 'subtitles',
        'mono_audio', 'audio_balance', 'noise_cancellation', 'echo_cancellation', 'spatial_audio',
        
        # Audio
        'surround_sound', 'bass_boost', 'treble_boost', 'equalizer', 'audio_normalization',
        
        # Power modes
        'data_saver', 'low_power', 'battery_saver', 'performance_mode', 'game_mode', 'reading_mode',
        
        # Profiles
        'work_profile', 'personal_profile', 'guest_mode', 'kids_mode', 'driving_mode', 'walking_mode',
        
        # Feedback
        'vibration', 'haptic_feedback', 'touch_sounds', 'screen_sounds', 'keyboard_sounds',
        'lock_sounds', 'charging_sounds', 'notification_led', 'flash_notifications', 'backlight',
        
        # Timeouts
        'key_backlight', 'timeout', 'screen_timeout', 'sleep_timeout', 'display_timeout',
        
        # Wake options
        'wake_lock', 'keep_awake', 'always_on_display', 'ambient_display', 'lift_to_wake',
        'double_tap_to_wake', 'double_tap_to_sleep', 'raise_to_wake', 'wave_to_wake', 'flip_to_mute',
        
        # Gestures
        'flip_to_snooze', 'shake_to_undo', 'pinch_to_zoom', 'swipe_to_type', 'glide_typing',
        'gesture_typing', 'one_handed_mode', 'split_screen', 'picture_in_picture', 'freeform'
    ]

    toggle_templates = [
        'toggle {}',           # Direct command
        'turn on {}',          # Enable phrasing
        'enable {}',           # Formal enable
        'switch {}',           # Alternative verb
        'activate {}',         # Activation verb
        'set {} on',           # State-based
        'activate {}',         # Duplicate for weighting (more common)
        'power on {}',         # Power phrasing
        'start {}',            # Begin verb
        'launch {}'            # Launch verb
    ]

    for setting in toggle_settings:
        template = random.choice(toggle_templates)
        samples.append(f'INPUT: {template.format(setting)} OUTPUT: {{"action": "toggle", "setting": "{setting}"}}')

    # ==========================================================================
    # SYSTEM COMMANDS (~100 samples)
    # ==========================================================================
    # Built-in navigation and control commands.
    # These don't have additional parameters - just the action name.
    # 
    # Patterns:
    # {"action": "back"}     - Go back to previous screen
    # {"action": "refresh"}  - Refresh/reload current content
    # {"action": "close"}    - Close/exit the app
    # {"action": "cancel"}   - Cancel current operation
    # {"action": "stop"}     - Stop current operation
    # {"action": "pause"}    - Pause current operation
    # {"action": "resume"}   - Resume paused operation
    # {"action": "play"}     - Start playback
    # {"action": "start"}    - Start something
    # {"action": "restart"}  - Restart app/process
    # {"action": "clear"}    - Clear content
    # {"action": "reset"}    - Reset to defaults
    # {"action": "delete"}   - Delete content
    # ==========================================================================
    
    system_commands = [
        # Back navigation - many ways to say "go back"
        ('go back', 'back'), ('back', 'back'), ('return', 'back'), ('go back one', 'back'), ('previous', 'back'),
        ('go back now', 'back'), ('take me back', 'back'), ('navigate back', 'back'), ('step back', 'back'), ('undo', 'back'),
        
        # Refresh/reload - synonym pairs
        ('refresh', 'refresh'), ('reload', 'refresh'), ('refresh page', 'refresh'), ('reload page', 'refresh'),
        ('refresh now', 'refresh'), ('reload now', 'refresh'), ('refresh content', 'refresh'), ('reload content', 'refresh'),
        ('update view', 'refresh'), ('sync now', 'refresh'),
        
        # Close/exit - ways to close the app
        ('close', 'close'), ('exit', 'close'), ('close app', 'close'), ('exit app', 'close'),
        ('quit', 'close'), ('close now', 'close'), ('exit now', 'close'), ('quit now', 'close'),
        ('close window', 'close'), ('exit window', 'close'),
        
        # Cancel/stop - interrupt operations
        ('cancel', 'cancel'), ('stop', 'stop'), ('abort', 'cancel'), ('cancel now', 'cancel'),
        ('stop now', 'stop'), ('halt', 'stop'), ('end', 'stop'), ('terminate', 'stop'),
        
        # Pause/resume - media control
        ('pause', 'pause'), ('resume', 'resume'), ('pause now', 'pause'), ('resume now', 'resume'),
        
        # Play/start - begin operations
        ('play', 'play'), ('start', 'start'), ('begin', 'start'), ('launch', 'start'),
        
        # Restart - reset operations
        ('restart', 'restart'), ('reboot', 'restart'), ('restart app', 'restart'), ('reboot app', 'restart'),
        
        # Clear/reset - content management
        ('clear', 'clear'), ('reset', 'reset'), ('clear all', 'clear'), ('reset all', 'reset'),
        
        # Delete/remove - content removal
        ('delete', 'delete'), ('remove', 'delete'), ('delete all', 'delete'), ('remove all', 'delete')
    ]

    for cmd, action in system_commands:
        samples.append(f'INPUT: {cmd} OUTPUT: {{"action": "{action}"}}')

    # ==========================================================================
    # UNRECOGNIZED INPUTS (~100 samples)
    # ==========================================================================
    # These are inputs that don't match any command pattern.
    # The model should recognize them as non-commands and echo them back.
    # 
    # Pattern: {"action": "unrecognized", "input": "<original input>"}
    # ==========================================================================
    
    unrecognized_inputs = [
        # Greetings
        'hello', 'hi', 'hey', 'good morning', 'good afternoon', 'good evening', 'good night',
        
        # Farewells
        'what is this', 'help', 'thanks', 'thank you', 'goodbye', 'bye', 'see you', 'later',
        
        # Conversational
        'how are you', 'how do you do', 'whats up', 'sup', 'yo', 'greetings', 'salutations',
        
        # Random/gibberish
        'test', 'testing', 'one two three', 'asdf', 'qwerty', 'random', 'blah', 'hmm', 'umm',
        
        # Non-committal
        'i dont know', 'never mind', 'forget it', 'skip', 'pass', 'ignore this', 'whatever',
        
        # Affirmations/negations
        'yes', 'no', 'maybe', 'ok', 'okay', 'sure', 'fine', 'alright', 'cool', 'nice',
        
        # Questions
        'what', 'why', 'how', 'when', 'where', 'who', 'which', 'whose', 'whom',
        
        # Requests the model can't handle
        'tell me a joke', 'sing a song', 'dance', 'play music', 'what time is it', 'what day is it',
        
        # Meta questions
        'who are you', 'what are you', 'where am i', 'what can you do', 'introduce yourself',
        
        # Celebrations
        'happy birthday', 'merry christmas', 'happy new year', 'congratulations', 'good luck',
        
        # Compliments/complaints
        'i love you', 'you suck', 'you are awesome', 'you are terrible', 'you are the best',
        
        # Confusion
        'this is confusing', 'i am lost', 'help me', 'i need help', 'emergency', 'urgent',
        
        # Test inputs
        'foo bar', 'lorem ipsum', 'test test test', '123456', 'abc123', 'password', 'admin',
        
        # Connectivity checks
        'can you hear me', 'are you there', 'hello world', 'ping', 'pong', 'echo', 'repeat',
        
        # Pop culture / jokes
        'make me a sandwich', 'open the pod bay doors', 'beam me up', 'engage', 'make it so'
    ]

    for inp in unrecognized_inputs:
        samples.append(f'INPUT: {inp} OUTPUT: {{"action": "unrecognized", "input": "{inp}"}}')

    # ==========================================================================
    # SHUFFLE AND RETURN
    # ==========================================================================
    # Shuffle to mix different command types together
    # This prevents the model from learning order-based patterns
    random.shuffle(samples)
    return samples


if __name__ == '__main__':
    samples = generate_dataset()
    with open('dataset.txt', 'w') as f:
        for sample in samples:
            f.write(sample + '\n')
    print(f'Generated {len(samples)} unique samples')