import { useState, useEffect } from 'react';
import { itemsApi } from '../api/api';
import {
  Package, Wrench, Cpu, Sprout, Dumbbell,
  BookOpen, UtensilsCrossed, Sofa, Gamepad2,
} from 'lucide-react';

const CATEGORY_ICON = {
  Tools: Wrench,
  Electronics: Cpu,
  Garden: Sprout,
  Sports: Dumbbell,
  Books: BookOpen,
  Kitchen: UtensilsCrossed,
  Furniture: Sofa,
  Toys: Gamepad2,
};

function ItemCard({ item, index = 0, footer, ownerName }) {
  const [imgSrc, setImgSrc] = useState(null);

  useEffect(() => {
    let active = true;
    let url;
    if (item.hasImage) {
      itemsApi.getItemImage(item.id)
        .then((res) => {
          if (!active) return;
          url = URL.createObjectURL(res.data);
          setImgSrc(url);
        })
        .catch(() => {});
    }
    return () => {
      active = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [item.id, item.hasImage]);

  const Icon = CATEGORY_ICON[item.category] || Package;

  return (
    <div className="surface rounded-2xl overflow-hidden flex flex-col hover:shadow-2xl hover:-translate-y-1 transition-all duration-300"
         style={{ animation: `fadeInUp 0.5s ease-out ${index * 0.06}s both` }}>
      {/* Media */}
      <div className="relative h-40 sm:h-44 bg-gradient-to-br from-emerald-500/15 to-teal-500/10 flex items-center justify-center">
        {imgSrc ? (
          <img src={imgSrc} alt={item.name} className="w-full h-full object-cover" />
        ) : (
          <Icon className="w-12 h-12 text-emerald-500/50" />
        )}
        <span className={`chip absolute top-3 right-3 backdrop-blur ${
          item.available
            ? 'bg-emerald-100/90 text-emerald-800 dark:bg-emerald-900/70 dark:text-emerald-200'
            : 'bg-gray-200/90 text-gray-700 dark:bg-gray-800/90 dark:text-gray-300'
        }`}>
          <span className={`w-1.5 h-1.5 rounded-full ${item.available ? 'bg-emerald-500' : 'bg-gray-400'}`} />
          {item.available ? 'Available' : 'In use'}
        </span>
      </div>

      {/* Body */}
      <div className="p-4 sm:p-5 flex flex-col flex-1">
        <span className="chip self-start bg-emerald-50 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300 mb-2">
          <Icon className="w-3.5 h-3.5" />
          {item.category}
        </span>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white leading-snug break-words">
          {item.name}
        </h3>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-2 flex-1">
          {item.description || 'No description provided.'}
        </p>
        {ownerName && (
          <p className="mt-3 text-xs text-gray-500 dark:text-gray-500">Shared by {ownerName}</p>
        )}
        {footer && <div className="mt-4">{footer}</div>}
      </div>
    </div>
  );
}

export default ItemCard;
