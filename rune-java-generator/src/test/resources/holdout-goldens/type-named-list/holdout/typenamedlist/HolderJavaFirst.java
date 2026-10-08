package holdout.typenamedlist;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import holdout.typenamedlist.meta.HolderJavaFirstMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A sibling whose FIRST attribute is a multi string: java.util.List claims the simple name first (imported), the model List is written fully qualified after it.
 * @version 0.0.0
 */
@RosettaDataType(value="HolderJavaFirst", builder=HolderJavaFirst.HolderJavaFirstBuilderImpl.class, version="0.0.0")
@RuneDataType(value="HolderJavaFirst", model="holdout", builder=HolderJavaFirst.HolderJavaFirstBuilderImpl.class, version="0.0.0")
public interface HolderJavaFirst extends RosettaModelObject {

	HolderJavaFirstMeta metaData = new HolderJavaFirstMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getNames();
	holdout.typenamedlist.List getSubject();
	List<? extends holdout.typenamedlist.List> getOthers();

	/*********************** Build Methods  ***********************/
	HolderJavaFirst build();
	
	HolderJavaFirst.HolderJavaFirstBuilder toBuilder();
	
	static HolderJavaFirst.HolderJavaFirstBuilder builder() {
		return new HolderJavaFirst.HolderJavaFirstBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends HolderJavaFirst> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends HolderJavaFirst> getType() {
		return HolderJavaFirst.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		processRosetta(path.newSubPath("subject"), processor, holdout.typenamedlist.List.class, getSubject());
		processRosetta(path.newSubPath("others"), processor, holdout.typenamedlist.List.class, getOthers());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderJavaFirstBuilder extends HolderJavaFirst, RosettaModelObjectBuilder {
		List.ListBuilder getOrCreateSubject();
		@Override
		List.ListBuilder getSubject();
		List.ListBuilder getOrCreateOthers(int index);
		@Override
		List<? extends List.ListBuilder> getOthers();
		HolderJavaFirst.HolderJavaFirstBuilder addNames(String names);
		HolderJavaFirst.HolderJavaFirstBuilder addNames(String names, int idx);
		HolderJavaFirst.HolderJavaFirstBuilder addNames(List<String> names);
		HolderJavaFirst.HolderJavaFirstBuilder setNames(List<String> names);
		HolderJavaFirst.HolderJavaFirstBuilder setSubject(holdout.typenamedlist.List subject);
		HolderJavaFirst.HolderJavaFirstBuilder addOthers(holdout.typenamedlist.List others);
		HolderJavaFirst.HolderJavaFirstBuilder addOthers(holdout.typenamedlist.List others, int idx);
		HolderJavaFirst.HolderJavaFirstBuilder addOthers(List<? extends holdout.typenamedlist.List> others);
		HolderJavaFirst.HolderJavaFirstBuilder setOthers(List<? extends holdout.typenamedlist.List> others);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
			processRosetta(path.newSubPath("subject"), processor, List.ListBuilder.class, getSubject());
			processRosetta(path.newSubPath("others"), processor, List.ListBuilder.class, getOthers());
		}
		

		HolderJavaFirst.HolderJavaFirstBuilder prune();
	}

	/*********************** Immutable Implementation of HolderJavaFirst  ***********************/
	class HolderJavaFirstImpl implements HolderJavaFirst {
		private final List<String> names;
		private final holdout.typenamedlist.List subject;
		private final List<? extends holdout.typenamedlist.List> others;
		
		protected HolderJavaFirstImpl(HolderJavaFirst.HolderJavaFirstBuilder builder) {
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.subject = ofNullable(builder.getSubject()).map(f->f.build()).orElse(null);
			this.others = ofNullable(builder.getOthers()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@Override
		@RosettaAttribute("subject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subject")
		public holdout.typenamedlist.List getSubject() {
			return subject;
		}
		
		@Override
		@RosettaAttribute("others")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("others")
		public List<? extends holdout.typenamedlist.List> getOthers() {
			return others;
		}
		
		@Override
		public HolderJavaFirst build() {
			return this;
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder toBuilder() {
			HolderJavaFirst.HolderJavaFirstBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(HolderJavaFirst.HolderJavaFirstBuilder builder) {
			ofNullable(getNames()).ifPresent(builder::setNames);
			ofNullable(getSubject()).ifPresent(builder::setSubject);
			ofNullable(getOthers()).ifPresent(builder::setOthers);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderJavaFirst _that = getType().cast(o);
		
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			if (!Objects.equals(subject, _that.getSubject())) return false;
			if (!ListEquals.listEquals(others, _that.getOthers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			_result = 31 * _result + (subject != null ? subject.hashCode() : 0);
			_result = 31 * _result + (others != null ? others.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderJavaFirst {" +
				"names=" + this.names + ", " +
				"subject=" + this.subject + ", " +
				"others=" + this.others +
			'}';
		}
	}

	/*********************** Builder Implementation of HolderJavaFirst  ***********************/
	class HolderJavaFirstBuilderImpl implements HolderJavaFirst.HolderJavaFirstBuilder {
	
		protected List<String> names = new ArrayList<>();
		protected List.ListBuilder subject;
		protected List<List.ListBuilder> others = new ArrayList<>();
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@Override
		@RosettaAttribute("subject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subject")
		public List.ListBuilder getSubject() {
			return subject;
		}
		
		@Override
		public List.ListBuilder getOrCreateSubject() {
			List.ListBuilder result;
			if (subject!=null) {
				result = subject;
			}
			else {
				result = subject = holdout.typenamedlist.List.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("others")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("others")
		public List<? extends List.ListBuilder> getOthers() {
			return others;
		}
		
		@Override
		public List.ListBuilder getOrCreateOthers(int index) {
			if (others==null) {
				this.others = new ArrayList<>();
			}
			return getIndex(others, index, () -> {
						List.ListBuilder newOthers = holdout.typenamedlist.List.builder();
						return newOthers;
					});
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("names")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addNames(List<String> namess) {
			if (namess != null) {
				for (final String toAdd : namess) {
					this.names.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("names")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder setNames(List<String> namess) {
			if (namess == null) {
				this.names = new ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("subject")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("subject")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder setSubject(holdout.typenamedlist.List _subject) {
			this.subject = _subject == null ? null : _subject.toBuilder();
			return this;
		}
		
		@RosettaAttribute("others")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("others")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addOthers(holdout.typenamedlist.List _others) {
			if (_others != null) {
				this.others.add(_others.toBuilder());
			}
			return this;
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addOthers(holdout.typenamedlist.List _others, int idx) {
			getIndex(this.others, idx, () -> _others.toBuilder());
			return this;
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder addOthers(List<? extends holdout.typenamedlist.List> otherss) {
			if (otherss != null) {
				for (final holdout.typenamedlist.List toAdd : otherss) {
					this.others.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("others")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("others")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder setOthers(List<? extends holdout.typenamedlist.List> otherss) {
			if (otherss == null) {
				this.others = new ArrayList<>();
			} else {
				this.others = otherss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public HolderJavaFirst build() {
			return new HolderJavaFirst.HolderJavaFirstImpl(this);
		}
		
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder prune() {
			if (subject!=null && !subject.prune().hasData()) subject = null;
			others = others.stream().filter(b->b!=null).<List.ListBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNames()!=null && !getNames().isEmpty()) return true;
			if (getSubject()!=null && getSubject().hasData()) return true;
			if (getOthers()!=null && getOthers().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderJavaFirst.HolderJavaFirstBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			HolderJavaFirst.HolderJavaFirstBuilder o = (HolderJavaFirst.HolderJavaFirstBuilder) other;
			
			merger.mergeRosetta(getSubject(), o.getSubject(), this::setSubject);
			merger.mergeRosetta(getOthers(), o.getOthers(), this::getOrCreateOthers);
			
			merger.mergeBasic(getNames(), o.getNames(), (Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderJavaFirst _that = getType().cast(o);
		
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			if (!Objects.equals(subject, _that.getSubject())) return false;
			if (!ListEquals.listEquals(others, _that.getOthers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			_result = 31 * _result + (subject != null ? subject.hashCode() : 0);
			_result = 31 * _result + (others != null ? others.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderJavaFirstBuilder {" +
				"names=" + this.names + ", " +
				"subject=" + this.subject + ", " +
				"others=" + this.others +
			'}';
		}
	}
}
