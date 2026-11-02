"""
plot_data_structures.py - Vẽ sơ đồ cấu trúc dữ liệu các thuật toán Hash Table.
Phục vụ bài báo IEEE CSD201 FA26 - Đề tài 2.
"""

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.lines import Line2D

# Font settings for Vietnamese support if needed
plt.rcParams['font.sans-serif'] = ['Arial', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

def draw_separate_chaining(ax):
    """Vẽ cấu trúc Separate Chaining: Bucket Array -> Linked List Nodes"""
    ax.set_title("(a) Separate Chaining", fontsize=14, fontweight='bold')
    ax.axis('off')
    
    # Bucket array
    buckets = [0, 1, 2, 3]
    y_bucket = 5
    for i, b in enumerate(buckets):
        rect = patches.Rectangle((i*1.5, y_bucket), 1.2, 0.8, linewidth=2, edgecolor='#1f77b4', facecolor='#e6f2ff')
        ax.add_patch(rect)
        ax.text(i*1.5 + 0.6, y_bucket + 0.4, f'Bucket {b}', ha='center', va='center', fontsize=10)
        
        # Draw linked list nodes
        node_x = i * 1.5 + 0.6
        node_y = y_bucket - 1.0
        for j in range(3 if i < 2 else 1):
            n_rect = patches.FancyBboxPatch((node_x - 0.5, node_y - 0.3), 1.0, 0.6, boxstyle="round,pad=0.1", 
                                           linewidth=1.5, edgecolor='#2ca02c', facecolor='#e6ffe6')
            ax.add_patch(n_rect)
            ax.text(node_x, node_y, f'K,V\n(DIB=N/A)', ha='center', va='center', fontsize=8)
            
            if j < (2 if i < 2 else 0):
                ax.annotate('', xy=(node_x, node_y - 0.9), xytext=(node_x, node_y - 0.3),
                           arrowprops=dict(arrowstyle='->', color='#2ca02c'))
            node_y -= 0.9
            
        # Arrow from bucket to first node
        if i < 2:
            ax.annotate('', xy=(node_x, y_bucket - 0.1), xytext=(node_x, y_bucket),
                       arrowprops=dict(arrowstyle='->', color='#1f77b4'))

def draw_linear_probing_tombstone(ax):
    """Vẽ cấu trúc Linear Probing Tombstone: Flat Array with EMPTY/OCCUPIED/TOMB"""
    ax.set_title("(b) Linear Probing Tombstone", fontsize=14, fontweight='bold')
    ax.axis('off')
    
    slots = ['OCC', 'TOMB', 'OCC', 'EMPTY', 'OCC']
    colors = ['#e6ffe6', '#ffd6d6', '#e6ffe6', '#f0f0f0', '#e6ffe6']
    borders = ['#2ca02c', '#d62728', '#2ca02c', '#cccccc', '#2ca02c']
    labels = ['K,V', 'TOMB', 'K,V', '', 'K,V']
    
    y = 3
    for i, (slot, col, bord, lbl) in enumerate(zip(slots, colors, borders, labels)):
        x = i * 1.5
        rect = patches.Rectangle((x, y), 1.2, 0.8, linewidth=2, edgecolor=bord, facecolor=col)
        ax.add_patch(rect)
        ax.text(x + 0.6, y + 0.4, f'Idx {i}\n{lbl}', ha='center', va='center', fontsize=9)
        
    # Legend
    legend_elements = [
        patches.Patch(facecolor='#e6ffe6', edgecolor='#2ca02c', label='OCCUPIED'),
        patches.Patch(facecolor='#ffd6d6', edgecolor='#d62728', label='TOMBSTONE'),
        patches.Patch(facecolor='#f0f0f0', edgecolor='#cccccc', label='EMPTY')
    ]
    ax.legend(handles=legend_elements, loc='lower right', fontsize=9)

def draw_robin_hood(ax):
    """Vẽ cấu trúc Robin Hood: Parallel Arrays (Key, Value, DIB, State)"""
    ax.set_title("(c) Robin Hood Hashing", fontsize=14, fontweight='bold')
    ax.axis('off')
    
    rows = ['Keys[]', 'Values[]', 'DIBs[]', 'State[]']
    data = [
        ['k1', 'k2', 'k3', 'k4'],
        ['v1', 'v2', 'v3', 'v4'],
        ['0', '1', '0', '2'],
        ['OCC', 'OCC', 'OCC', 'OCC']
    ]
    
    y_start = 5
    for r_idx, row_name in enumerate(rows):
        y = y_start - r_idx * 1.0
        for c_idx in range(4):
            x = c_idx * 1.5
            is_dib = (r_idx == 2)
            face = '#fff3cd' if is_dib else '#e6f2ff'
            border = '#ffc107' if is_dib else '#1f77b4'
            
            rect = patches.Rectangle((x, y), 1.2, 0.8, linewidth=2, edgecolor=border, facecolor=face)
            ax.add_patch(rect)
            ax.text(x + 0.6, y + 0.4, data[r_idx][c_idx], ha='center', va='center', fontsize=10, 
                   fontweight='bold' if is_dib else 'normal')
        ax.text(-0.5, y + 0.4, row_name, ha='right', va='center', fontsize=10, fontweight='bold')
        
    # Annotation for DIB swap mechanism
    ax.annotate('DIB Swap:\nRich gives to Poor', xy=(2.1, 3.4), xytext=(4.5, 4.5),
               arrowprops=dict(arrowstyle='->', color='#d62728', lw=2),
               fontsize=9, color='#d62728', bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

def draw_lp_backshift(ax):
    """Vẽ cấu trúc LP Backshift: Flat Array without Tombstones"""
    ax.set_title("(d) LP Backshift (Knuth R)", fontsize=14, fontweight='bold')
    ax.axis('off')
    
    slots = ['OCC', 'OCC', 'EMPTY', 'OCC', 'OCC']
    colors = ['#e6ffe6', '#e6ffe6', '#f0f0f0', '#e6ffe6', '#e6ffe6']
    borders = ['#2ca02c', '#2ca02c', '#cccccc', '#2ca02c', '#2ca02c']
    labels = ['K,V\n(h=0)', 'K,V\n(h=1)', '', 'K,V\n(h=2)', 'K,V\n(h=2)']
    
    y = 3
    for i, (slot, col, bord, lbl) in enumerate(zip(slots, colors, borders, labels)):
        x = i * 1.5
        rect = patches.Rectangle((x, y), 1.2, 0.8, linewidth=2, edgecolor=bord, facecolor=col)
        ax.add_patch(rect)
        ax.text(x + 0.6, y + 0.4, f'Idx {i}\n{lbl}', ha='center', va='center', fontsize=8)
        
    # Show backshift deletion arrow
    ax.annotate('Backshift Deletion:\nShift left until DIB==0 or EMPTY', 
               xy=(2.1, 3.0), xytext=(0.5, 1.0),
               arrowprops=dict(arrowstyle='->', color='#ff7f0e', lw=2),
               fontsize=9, color='#ff7f0e', bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

def main():
    fig, axs = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle('Data Structure Comparison: Hash Table Collision Resolution Strategies\n(CSD201 FA26 - Group 1)', 
                 fontsize=16, fontweight='bold', y=0.98)
    
    draw_separate_chaining(axs[0, 0])
    draw_linear_probing_tombstone(axs[0, 1])
    draw_robin_hood(axs[1, 0])
    draw_lp_backshift(axs[1, 1])
    
    plt.tight_layout(rect=[0, 0, 1, 0.95])
    
    out_path = 'results/figures/data_structure_comparison.png'
    plt.savefig(out_path, dpi=300, bbox_inches='tight')
    print(f"[SAVED] Sơ đồ cấu trúc dữ liệu: {out_path}")
    plt.show()

if __name__ == '__main__':
    main()