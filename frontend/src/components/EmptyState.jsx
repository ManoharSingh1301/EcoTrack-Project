function EmptyState({ icon: Icon, title, message, action }) {
  return (
    <div className="surface rounded-2xl p-10 sm:p-14 text-center">
      {Icon && <Icon className="w-14 h-14 mx-auto mb-4 text-gray-300 dark:text-gray-600" />}
      <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">{title}</h3>
      {message && <p className="mt-1 text-gray-500 dark:text-gray-400">{message}</p>}
      {action && <div className="mt-5 flex justify-center">{action}</div>}
    </div>
  );
}

export default EmptyState;
